package com.example.SmartStay.user;

import com.example.SmartStay.email.EmailService;
import com.example.SmartStay.product.Product;
import com.example.SmartStay.product.ProductService;
import com.example.SmartStay.reservation.Reservation;
import com.example.SmartStay.reservation.ReservationService;
import com.example.SmartStay.review.Review;
import com.example.SmartStay.review.ReviewProjection;
import com.example.SmartStay.review.ReviewService;
import com.example.SmartStay.util.JwtUtil;
import com.example.SmartStay.util.UserUtil;
import com.example.SmartStay.websocket.WebSocketService;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserUtil userUtil;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReviewService reviewService;

    @Value("${env.URL}")
    private String URL;

    // Security for login and register
    private final ConcurrentHashMap<String, Pair<Integer, Long>> requestCounts = new ConcurrentHashMap<>();
    private final long RATE_LIMIT_TIME_WINDOW = TimeUnit.MINUTES.toMillis(1);

    @GetMapping
    public ResponseEntity<List<UserProjection>> allUsers(@RequestHeader("Authorization") String token) {
        if (!userService.isAdmin(token)) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        List<UserProjection> users = userService.getAllUsersProjected();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String token) {
        boolean isValid = jwtUtil.isValidToken(token);
        if (!isValid) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        // check if admin is the same as in the token
        boolean isAdmin = userService.isAdmin(token);
        boolean isAdminInToken = jwtUtil.isAdmin(token);

        if (isAdminInToken != isAdmin) {
            // tells the client to renovate the token
            return new ResponseEntity<>("null", HttpStatus.ACCEPTED);
        }

        return new ResponseEntity<>("null", HttpStatus.OK);
    }

    @GetMapping("/renovate")
    public ResponseEntity<String> renovateToken(@RequestHeader("Authorization") String token) {
        User user = userUtil.getValidUser(token);
        if (user == null) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        String newToken = jwtUtil.generateToken(user);
        return new ResponseEntity<>(newToken, HttpStatus.OK);
    }

    @GetMapping("/wishlist")
    public ResponseEntity<List<String>> getWishlist(@RequestHeader("Authorization") String token) {
        User user = userUtil.getValidUser(token);
        if (user == null) {
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(user.getWishlist(), HttpStatus.OK);
    }

    @PostMapping("/wishlist/add/{id}")
    public ResponseEntity<String> addToWishlist(@PathVariable ObjectId id, @RequestHeader("Authorization") String token) {
        User user = userUtil.getValidUser(token);
        if (user == null) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        // return 404 if product not found
        Optional<Product> product = productService.getProductById(id);
        if (product.isEmpty()) {
            return new ResponseEntity<>("null", HttpStatus.NOT_FOUND);
        }

        if (user.getWishlist().contains(product.get().getId())) {
            return new ResponseEntity<>("null", HttpStatus.CONFLICT);
        }

        // add product to wishlist
        user.getWishlist().add(product.get().getId());
        userService.save(user);

        // send webhook to get wishlist
        webSocketService.sendMessage("updateWishlist", List.of(user.getId()));
        return new ResponseEntity<>("null", HttpStatus.OK);
    }

    @DeleteMapping("/wishlist/remove/{id}")
    public ResponseEntity<String> removeFromWishlist(@PathVariable ObjectId id, @RequestHeader("Authorization") String token) {
       User user = userUtil.getValidUser(token);
        if (user == null) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        // remove product from wishlist
        user.getWishlist().remove(id.toString());
        userService.save(user);

        // send webhook to get wishlist
        webSocketService.sendMessage("updateWishlist", List.of(user.getId()));
        return new ResponseEntity<>("null", HttpStatus.OK);
    }

    @PostMapping("/register")
    public ResponseEntity<String> createUser(@RequestBody CreateUserRequest createUserRequest, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        if (isRateLimited(clientIp)) {
            return new ResponseEntity<>("null", HttpStatus.TOO_MANY_REQUESTS);
        }

        // check valid email, password, first name and last name
        if (!isValidEmail(createUserRequest.getEmail()) || !isValidPassword(createUserRequest.getPassword()) ||
                !isValidName(createUserRequest.getFirstName()) || !isValidName(createUserRequest.getLastName())) {
            return new ResponseEntity<>("null", HttpStatus.BAD_REQUEST);
        }

        // try to find other user with the same username
        List<User> users = userService.getAllUsers();
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(createUserRequest.getEmail())) {
                if (!u.isConfirmed()) {
                    // send email again
                    sendEmail(u);
                    return new ResponseEntity<>("null", HttpStatus.ACCEPTED);
                }

                return new ResponseEntity<>("null", HttpStatus.CONFLICT);
            }
        }

        try {
            byte[] hashedPassword = Password.hashPassword(createUserRequest.getPassword());

            User user = new User(
                    createUserRequest.getEmail(),
                    hashedPassword,
                    false,
                    createUserRequest.getFirstName(),
                    createUserRequest.getLastName(),
                    false
            );

            userService.save(user);

            // send confirmation email
            sendEmail(user);
            return new ResponseEntity<>("null", HttpStatus.CREATED);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("NoSuchAlgorithmException was thrown in createUser", e);
            return new ResponseEntity<>("null", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sendEmail(User user) {
        String confirmToken = jwtUtil.generateConfirmUserToken(user.getEmail());
        String confirmLink = URL + "/api/users/confirm/" + confirmToken;
        // replace every ${confirmLink} with confirmLink
        String emailTemplate = emailService.getTemplate("welcome");
        emailTemplate = emailTemplate.replace("${name}", user.getFirstName());
        emailTemplate = emailTemplate.replace("${confirmationLink}", confirmLink);
        emailService.sendEmail(user.getEmail(), "Bienvenido a SmartStay: Confirmar email", emailTemplate);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginUserRequest loginUserRequest, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        if (isRateLimited(clientIp)) {
            return new ResponseEntity<>("null", HttpStatus.TOO_MANY_REQUESTS);
        }

        // check valid email
        if (!isValidEmail(loginUserRequest.getEmail()) || !isValidPassword(loginUserRequest.getPassword())) {
            return new ResponseEntity<>("null", HttpStatus.BAD_REQUEST);
        }

        List<User> users = userService.getAllUsers();
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(loginUserRequest.getEmail())) {
                try {
                    if (Password.verifyPassword(loginUserRequest.getPassword(), u.getPassword())) {
                        // check if email is confirmed
                        if (!u.isConfirmed()) {
                            return new ResponseEntity<>("null", HttpStatus.FORBIDDEN);
                        }

                        // create jwt and send
                        String token = jwtUtil.generateToken(u);
                        return new ResponseEntity<>(token, HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
                    }
                } catch (NoSuchAlgorithmException e) {
                    LOGGER.error("NoSuchAlgorithmException was thrown in login", e);
                    return new ResponseEntity<>("null", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }
        return new ResponseEntity<>("null", HttpStatus.NOT_FOUND);
    }

    @GetMapping("/confirm/{token}")
    public ResponseEntity<String> confirmEmail(@PathVariable String token) {
        if (!jwtUtil.verifyConfirmUserToken(token)) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtUtil.getEmailFromConfirmUserToken(token);
        List<User> users = userService.getAllUsers();
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(email)) {
                if (!u.isConfirmed()) {
                    u.setConfirmed(true);
                    userService.save(u);
                }
                return new ResponseEntity<>("Bienvenido a SmartStay: ya puedes volver a nuestra pagina web y Loguearte", HttpStatus.OK);
            }
        }
        return new ResponseEntity<>("Bienvenido a SmartStay: ya puedes volver a nuestra pagina web y Loguearte", HttpStatus.NOT_FOUND);
    }

    @PostMapping("/update/name")
    public ResponseEntity<String> updateUser(@RequestBody UpdateUserNameRequest updateUserRequest, @RequestHeader("Authorization") String token) {
        if (!jwtUtil.isValidToken(token)) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        // get user from jwt
        String email = jwtUtil.getEmail(token);
        if (email == null) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        // check if names are valid
        if (!isValidName(updateUserRequest.getFirstName()) || !isValidName(updateUserRequest.getLastName())) {
            return new ResponseEntity<>("null", HttpStatus.BAD_REQUEST);
        }

        List<User> users = userService.getAllUsers();
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(email)) {
                // update user names
                u.setFirstName(updateUserRequest.getFirstName());
                u.setLastName(updateUserRequest.getLastName());
                userService.save(u);

                // generate new jwt and set it
                String newToken = jwtUtil.generateToken(u);
                return new ResponseEntity<>(newToken, HttpStatus.OK);
            }
        }
        return new ResponseEntity<>("null", HttpStatus.NOT_FOUND);
    }

    @PostMapping("/update/password")
    public ResponseEntity<String> updatePassword(@RequestBody UpdateUserPasswordRequest updatePasswordRequest, @RequestHeader("Authorization") String token) {
        if (!jwtUtil.isValidToken(token)) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        // get user from jwt
        String email = jwtUtil.getEmail(token);
        if (email == null) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        // check if password is valid
        if (!isValidPassword(updatePasswordRequest.getNewPassword())) {
            return new ResponseEntity<>("null", HttpStatus.BAD_REQUEST);
        }

        List<User> users = userService.getAllUsers();
        for (User u : users) {
            if (u.getEmail().equalsIgnoreCase(email)) {
                try {
                    // check if old password is correct
                    if (Password.verifyPassword(updatePasswordRequest.getOldPassword(), u.getPassword())) {
                        // update password
                        u.setPassword(Password.hashPassword(updatePasswordRequest.getNewPassword()));
                        userService.save(u);

                        // send email to user using html template
                        String emailTemplate = emailService.getTemplate("password");
                        emailTemplate = emailTemplate.replace("${name}", u.getFirstName());
                        emailService.sendEmail(u.getEmail(), "SmartStay: Contraseña actualizada", emailTemplate);
                        return new ResponseEntity<>("null", HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>("null", HttpStatus.I_AM_A_TEAPOT);
                    }
                } catch (NoSuchAlgorithmException e) {
                    LOGGER.error("NoSuchAlgorithmException was thrown in updatePassword", e);
                    return new ResponseEntity<>("null", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            }
        }
        return new ResponseEntity<>("null", HttpStatus.NOT_FOUND);
    }

    @PostMapping("/update/setAdmin")
    public ResponseEntity<String> setAdmin(@RequestBody SetAdminRequest setAdminRequest, @RequestHeader("Authorization") String token) {
        if (!userService.isAdmin(token)) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        String userId = jwtUtil.getId(token);
        if (userId == null || !ObjectId.isValid(userId)) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        User user = userService.getUserById(new ObjectId(userId)).orElse(null);
        if (user == null) {
            return new ResponseEntity<>("null", HttpStatus.UNAUTHORIZED);
        }

        if (!ObjectId.isValid(setAdminRequest.getId())) {
            return new ResponseEntity<>("null", HttpStatus.BAD_REQUEST);
        }

        User userToUpdate = userService.getUserById(new ObjectId(setAdminRequest.getId())).orElse(null);
        if (userToUpdate == null) {
            return new ResponseEntity<>("null", HttpStatus.NOT_FOUND);
        }

        // cannot update own admin status
        if (Objects.equals(userToUpdate, user)) {
            return new ResponseEntity<>("null", HttpStatus.BAD_REQUEST);
        }

        LOGGER.info("Update admin status for user: " + userToUpdate.getEmail() + " to: " + setAdminRequest.getIsAdmin());
        userToUpdate.setAdmin(setAdminRequest.getIsAdmin());
        userService.save(userToUpdate);

        // send event to all clients to update jwt from this user
        webSocketService.sendMessage("updateUser", List.of(userToUpdate.getId()));
        return new ResponseEntity<>("null", HttpStatus.OK);
    }

    @GetMapping("/reservations")
    public ResponseEntity<List<Reservation>> getReservations(@RequestHeader("Authorization") String token) {
        User user = userUtil.getValidUser(token);
        if (user == null) {
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(reservationService.getReservationsByUserId(user.getId()), HttpStatus.OK);
    }

    @GetMapping("/reviews")
    public ResponseEntity<List<Review>> getReviews(@RequestHeader("Authorization") String token) {
        User user = userUtil.getValidUser(token);
        if (user == null) {
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(reviewService.getReviewsByUserId(user.getId()), HttpStatus.OK);
    }

    @GetMapping("/getName/{id}")
    public ResponseEntity<String> getName(@PathVariable String id) {
        Optional<User> user = userService.getUserById(new ObjectId(id));
        if (user.isEmpty()) {
            return new ResponseEntity<>("null", HttpStatus.NOT_FOUND);
        }

        if (!user.get().isConfirmed()) {
            return new ResponseEntity<>("null", HttpStatus.NOT_FOUND);
        }

        // check if user has at least one review
        // with this, we protect the user's privacy if they have not made any reviews
        // and someone tries to get their name
        List<ReviewProjection> reviews = reviewService.getReviewsByUserIdProjection(user.get().getId());
        if (reviews.isEmpty()) {
            return new ResponseEntity<>("null", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(user.get().getFirstName() + " " + user.get().getLastName(), HttpStatus.OK);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private boolean isRateLimited(String clientIp) {
        long currentTimeMillis = System.currentTimeMillis();
        // get request count
        Pair<Integer, Long> requestCountPair = requestCounts.getOrDefault(clientIp, Pair.of(0, 0L));
        long lastRequestTime = requestCountPair.getSecond();

        // 10 requests per minute (login and register)
        int MAX_REQUESTS_PER_WINDOW = 10;

        if (currentTimeMillis - lastRequestTime < RATE_LIMIT_TIME_WINDOW && requestCountPair.getFirst() < MAX_REQUESTS_PER_WINDOW) {
            requestCounts.put(clientIp, Pair.of(requestCountPair.getFirst() + 1, currentTimeMillis));
            return false;
        } else if (currentTimeMillis - lastRequestTime > RATE_LIMIT_TIME_WINDOW) {
            requestCounts.put(clientIp, Pair.of(1, currentTimeMillis));
            return false;
        }

        LOGGER.info("Rate limit exceeded for IP: " + clientIp);
        return true;
    }

    public static boolean isValidEmail(String email) {
        String emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        String passwordRegex = "^.{8,}$";
        Pattern pattern = Pattern.compile(passwordRegex);
        return pattern.matcher(password).matches();
    }

    public static boolean isValidName(String name) {
        String nameRegex = "^[a-zA-ZÀ-ÿ\\s]+$";
        Pattern pattern = Pattern.compile(nameRegex);
        return pattern.matcher(name).matches();
    }

}
