package com.example.SmartStay.product;

import com.example.SmartStay.category.Category;
import com.example.SmartStay.category.CategoryService;
import com.example.SmartStay.email.EmailService;
import com.example.SmartStay.reservation.Reservation;
import com.example.SmartStay.reservation.ReservationProjection;
import com.example.SmartStay.reservation.ReservationRequest;
import com.example.SmartStay.reservation.ReservationService;
import com.example.SmartStay.review.Review;
import com.example.SmartStay.review.ReviewProjection;
import com.example.SmartStay.review.ReviewRequest;
import com.example.SmartStay.review.ReviewService;
import com.example.SmartStay.user.User;
import com.example.SmartStay.user.UserService;
import com.example.SmartStay.util.JwtUtil;
import com.example.SmartStay.util.UserUtil;
import com.example.SmartStay.websocket.WebSocketService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserUtil userUtil;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<Product>> allProducts(@RequestHeader("Authorization") String token) {
        // check if is admin
        if (!userService.isAdmin(token)) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(productService.getAllProducts(), HttpStatus.OK);
    }

    @GetMapping("/home")
    public ResponseEntity<List<ProductRatingDTO>> homeProducts() {
        return new ResponseEntity<>(getRandomProducts(), HttpStatus.OK);
    }

    private List<ProductRatingDTO> getRandomProducts() {
        // all products
        List<Product> products = productService.getAllProducts();
        // shuffle products
        Collections.shuffle(products);
        int toIndex = Math.min(products.size(), 100);

        return transformProducts(products.subList(0, toIndex));
    }

    @GetMapping("/addresses")
    public ResponseEntity<HashSet<Address>> allAddresses() {
        return new ResponseEntity<>(productService.getAllAddresses(), HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductRatingDTO>> searchProducts(@RequestParam String query) {
        // check if query has comma, that means that is a country and city
        if (query.contains(",")) {
            String[] queries = query.split(",");
            if (queries.length == 2) {
                List<Product> products = productService.getProductsByCountryAndCity(queries[0].trim(), queries[1].trim());
                if (!products.isEmpty()) {
                    return new ResponseEntity<>(transformProducts(products), HttpStatus.OK);
                }
            }
        }

        query = cleanSearchString(query);

        // limit to 3 queries
        String[] queries = Arrays.stream(query.split(" ")).limit(3).toArray(String[]::new);

        if (query.isBlank() || queries.length == 0) {
            return new ResponseEntity<>(getRandomProducts(), HttpStatus.OK);
        }

        // if query only has one word, check if is a category
        if (queries.length == 1) {
            Category category = categoryService.getCategoryByName(queries[0]).orElse(null);
            if (category != null) {
                List<Product> products = Collections.emptyList();

                for (String productId : category.getProducts()) {
                    Optional<Product> product = productService.getProductById(new ObjectId(productId));
                    if (product.isPresent()) {
                        if (products.isEmpty()) {
                            products = new ArrayList<>();
                        }
                        products.add(product.get());
                    }
                }

                if (products.isEmpty()) {
                    return new ResponseEntity<>(getRandomProducts(), HttpStatus.OK);
                }

                return new ResponseEntity<>(transformProducts(products), HttpStatus.OK);
            }
        }

        // add the first query to the rest of the queries if there are less than 3
        while (queries.length < 3) {
            queries = Arrays.copyOf(queries, queries.length + 1);
            queries[queries.length - 1] = queries[0];
        }
        List<Product> products = productService.getProductsByCountryAndCityQuery(queries[0], queries[1], queries[2]);

        if (products.isEmpty()) {
            return new ResponseEntity<>(getRandomProducts(), HttpStatus.OK);
        }


        return new ResponseEntity<>(transformProducts(products), HttpStatus.OK);
    }

    private List<ProductRatingDTO> transformProducts(List<Product> products) {
        return products.stream()
                .map(product -> {
                    List<ReviewProjection> reviews = reviewService.getReviewsByProductIdProjection(product.getId());
                    double rating = reviews.stream().mapToDouble(ReviewProjection::getRating).average().orElse(0);
                    return new ProductRatingDTO(product, rating, reviews.size());
                })
                .toList();
    }

    @GetMapping("/wishlist")
    public ResponseEntity<List<ProductRatingDTO>> getWishlist(@RequestHeader("Authorization") String token) {
        User user = userUtil.getValidUser(token);
        if (user == null) {
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.UNAUTHORIZED);
        }

        List<String> productIds = user.getWishlist();
        if (productIds == null || productIds.isEmpty()) {
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);
        }

        List<Product> products = new ArrayList<>();
        for (String productId : productIds) {
            Optional<Product> product = productService.getProductById(new ObjectId(productId));
            product.ifPresent(products::add);
        }

        return new ResponseEntity<>(transformProducts(products), HttpStatus.OK);
    }

    public static String cleanSearchString(String search) {
        return search.replaceAll("[^a-zA-Z\\s]", "");
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<Optional<Product>> getProductById(@PathVariable ObjectId id) {
        // return 404 if product not found
        Optional<Product> product = productService.getProductById(id);
        if (product.isEmpty()) {
            return new ResponseEntity<>(product, HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @PostMapping("/add")
    public ResponseEntity<Product> addProduct(@RequestBody CreateProductRequest productRequest, @RequestHeader("Authorization") String token) {
        // check if is admin
        if (!userService.isAdmin(token)) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        // first check if product already exists by name
        List<Product> products = productService.getAllProducts();
        for (Product p : products) {
            if (p.getName().equals(productRequest.getName())) {
                return new ResponseEntity<>(p, HttpStatus.CONFLICT);
            }
        }

        Category category = null;

        // check if category exists
        if (ObjectId.isValid(productRequest.getCategoryId())) {
            category = categoryService.getCategoryById(new ObjectId(productRequest.getCategoryId())).orElse(null);
        }


        // create product
        Product product = new Product(
                productRequest.getName(),
                productRequest.getDescription(),
                productRequest.getImages(),
                productRequest.getFeatures(),
                productRequest.getAddress(),
                productRequest.getMapUrl(),
                productRequest.getPolicies(),
                productRequest.getMapEmbed()
        );

        productService.save(product);

        if (category != null) {
            // add product to category
            categoryService.addProductToCategory(new ObjectId(category.getId()), new ObjectId(product.getId()));
        }

        // send websocket message
        webSocketService.sendMessage("updateProduct", List.of(product.getId()));
        return new ResponseEntity<>(product, HttpStatus.CREATED);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable ObjectId id, @RequestHeader("Authorization") String token) {
        // check if is admin
        if (!userService.isAdmin(token)) {
            return new ResponseEntity<>("Unauthorized", HttpStatus.UNAUTHORIZED);
        }

        // return 404 if product not found
        Product product = productService.getProductById(id).orElse(null);
        if (product == null) {
            return new ResponseEntity<>("Product not found", HttpStatus.NOT_FOUND);
        }

        // get category
        Category category = categoryService.getCategoryByProductId(id);

        // remove product from category
        if (category != null) {
            categoryService.removeProductFromCategory(category, id);
        }

        productService.deleteProduct(id);

        // send websocket message
        webSocketService.sendMessage("deleteProduct", List.of(id.toString()));
        return new ResponseEntity<>("Product deleted", HttpStatus.OK);
    }

    @PostMapping("/edit")
    public ResponseEntity<Product> editProduct(@RequestBody UpdateProductRequest productRequest, @RequestHeader("Authorization") String token) {
        // check if is admin
        if (!userService.isAdmin(token)) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        if (!ObjectId.isValid(productRequest.getId())) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        ObjectId id = new ObjectId(productRequest.getId());

        // return 404 if product not found
        Product product = productService.getProductById(id).orElse(null);
        if (product == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        Category category = categoryService.getCategoryByProduct(product);

        // check if category id change
        if ((category != null && productRequest.getCategoryId().isBlank()) ||
            (category == null && !productRequest.getCategoryId().isBlank()) ||
                (category != null && !Objects.equals(category.getId(), productRequest.getCategoryId()))) {

            // remove product from old category
            if (category != null) {
                categoryService.removeProductFromCategory(category, id);
                webSocketService.sendMessage("updateCategory", List.of(category.getId()));
            }

            // check if new category exists and set
            if (ObjectId.isValid(productRequest.getCategoryId())) {
                if (categoryService.getCategoryById(new ObjectId(productRequest.getCategoryId())).isPresent()) {
                    // add product to new category
                    categoryService.addProductToCategory(new ObjectId(productRequest.getCategoryId()), id);
                    webSocketService.sendMessage("updateCategory", List.of(productRequest.getCategoryId()));
                }
            }
        }

        // update product
        Product updatedProduct = new Product(
                productRequest.getName(),
                productRequest.getDescription(),
                productRequest.getImages(),
                productRequest.getFeatures(),
                productRequest.getAddress(),
                productRequest.getMapUrl(),
                productRequest.getPolicies(),
                productRequest.getMapEmbed()
        );
        updatedProduct.setId(id.toString());
        productService.save(updatedProduct);

        // send websocket message
        webSocketService.sendMessage("updateProduct", List.of(id.toString()));
        return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
    }

    @PostMapping("/reserve")
    public ResponseEntity<Product> addReservation(@RequestBody ReservationRequest reservationRequest, @RequestHeader("Authorization") String token) {
        User user = userUtil.getValidUser(token);
        if (user == null) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        // return 404 if product not found
        Product product = productService.getProductById(reservationRequest.getProductId()).orElse(null);
        if (product == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        // check if reservation is valid
        if (reservationRequest.getStart() >= reservationRequest.getEnd()) {
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }

        // modify start and end to set startTime always at 2 P.M and endTime always at 11 A.M
        Long start = setStartOrEndTime(reservationRequest.getStart(), true);
        Long end = setStartOrEndTime(reservationRequest.getEnd(), false);

        // check if reservation is already taken
        List<Reservation> reservations = reservationService.getReservationsByProductId(product.getId());
        for (Reservation reservation : reservations) {
            if (start < reservation.getEnd() && end > reservation.getStart()) {
                return new ResponseEntity<>(null, HttpStatus.CONFLICT);
            }
        }

        Reservation reservation = new Reservation(user.getId(), product.getId(), start, end);
        reservationService.save(reservation);

        // send email to user
        String emailTemplate = emailService.getTemplate("reservation");
        emailTemplate = emailTemplate.replace("${name}", user.getFirstName());
        emailTemplate = emailTemplate.replace("${productName}", product.getName());
        long nights = (reservation.getEnd() - reservation.getStart()) / 86400000;
        emailTemplate = emailTemplate.replace("${nights}", "" + (nights + 1));

        // send date in spain format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy 'a las' HH:mm", new Locale("es", "ES"));
        LocalDateTime startDateTime = LocalDateTime.ofInstant(new Date(start).toInstant(), ZoneId.systemDefault());
        LocalDateTime endDateTime = LocalDateTime.ofInstant(new Date(end).toInstant(), ZoneId.systemDefault());
        String formattedStartDate = startDateTime.format(formatter);
        String formattedEndDate = endDateTime.format(formatter);

        emailTemplate = emailTemplate.replace("${startDate}", formattedStartDate);
        emailTemplate = emailTemplate.replace("${endDate}", formattedEndDate);
        emailService.sendEmail(user.getEmail(), "Reserva confirmada: " + product.getName(), emailTemplate);

        // send websocket message
        webSocketService.sendMessage("updateReservation", List.of(product.getId(), user.getId()));
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @GetMapping("/reservations/{id}")
    public ResponseEntity<List<ReservationProjection>> getReservations(@PathVariable ObjectId id) {
        return new ResponseEntity<>(reservationService.getReservationsByProductIdProjection(id.toString()), HttpStatus.OK);
    }

    @GetMapping("/getReviewProductId/{id}")
    public ResponseEntity<List<Review>> getReviewByProductId(@PathVariable ObjectId id) {
        return new ResponseEntity<>(reviewService.getReviewsByProductId(id.toString()), HttpStatus.OK);
    }

    @GetMapping("/getReviewById/{id}")
    public ResponseEntity<Review> getReviewById(@PathVariable ObjectId id) {
        return new ResponseEntity<>(reviewService.getReviewById(id.toString()), HttpStatus.OK);
    }

    @PostMapping("/review")
    public ResponseEntity<String> addReview(@RequestBody ReviewRequest reviewRequest, @RequestHeader("Authorization") String token) {
        User user = userUtil.getValidUser(token);
        if (user == null) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }

        // return 404 if product not found
        Product product = productService.getProductById(reviewRequest.getProductId()).orElse(null);
        if (product == null) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }

        // check if user already reviewed this product
        List<Review> reviews = reviewService.getReviewsByUserId(user.getId());
        for (Review review : reviews) {
            if (review.getProductId().equals(product.getId())) {
                return new ResponseEntity<>(null, HttpStatus.CONFLICT);
            }
        }

        Review review = new Review(product.getId(), user.getId(), reviewRequest.getRating(), reviewRequest.getComment());
        reviewService.save(review);

        // send websocket message
        webSocketService.sendMessage("createReview", List.of(product.getId(), user.getId(), review.getId()));
        return new ResponseEntity<>("created", HttpStatus.OK);
    }

    private Long setStartOrEndTime(Long date, boolean isStart) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        if (isStart) {
            calendar.set(Calendar.HOUR_OF_DAY, 14);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 11);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }
        // Convert to UTC
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        return calendar.getTimeInMillis();
    }
}
