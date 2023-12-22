package org.glygen.tablemaker.controller;

import org.glygen.tablemaker.view.SuccessResponse;
import org.glygen.tablemaker.view.UserStatisticsView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/data")
public class DataController {
    
    @Operation(summary = "Get data counts", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/statistics")
    public ResponseEntity<SuccessResponse> getStatistics() {
        UserStatisticsView stats = new UserStatisticsView();
        return new ResponseEntity<>(new SuccessResponse(stats, "statistics gathered"), HttpStatus.OK);
    }

}
