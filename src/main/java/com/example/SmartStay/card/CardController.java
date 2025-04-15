package com.example.SmartStay.card;

import com.example.SmartStay.product.Product;
import com.example.SmartStay.product.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class CardController {

    @Autowired
    ProductService productService;

    @Value("${env.URL}")
    private String URL;

    @GetMapping("/productCard")
    public String getProductCard(@RequestParam String id, Model model, HttpServletRequest request) {
        Optional<Product> product = productService.getProductById(new ObjectId(id));
        if (product.isEmpty()) {
            return "redirect:/";
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && !(userAgent.contains("Twitterbot") || userAgent.contains("facebookexternalhit"))) {
            // Redirect users to the product page
            return "redirect:/product?id=" + product.get().getId();
        }

        // set map url, used in html to redirect to product page
        product.get().setMapUrl(URL + "/product?id=" + product.get().getId());
        model.addAttribute("product", product.get());
        return "productCard";
    }
}