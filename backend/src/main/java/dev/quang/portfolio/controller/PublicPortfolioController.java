package dev.quang.portfolio.controller;

import dev.quang.portfolio.dto.PortfolioDtos.*;
import dev.quang.portfolio.service.PortfolioService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PublicPortfolioController {

    private final PortfolioService portfolioService;

    public PublicPortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @GetMapping("/portfolio")
    public PortfolioResponse getPortfolio() {
        return portfolioService.getPortfolio();
    }

    @PostMapping("/contact")
    @ResponseStatus(HttpStatus.CREATED)
    public ContactMessageDto sendContact(@Valid @RequestBody ContactRequest request) {
        return portfolioService.createContactMessage(request);
    }
}
