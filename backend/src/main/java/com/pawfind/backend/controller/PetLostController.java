
package com.pawfind.backend.controller;

import com.pawfind.backend.common.exception.InvalidCredentialsException;
import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.dto.PetLostCreateDTO;
import com.pawfind.backend.service.PetLostService;
import com.pawfind.backend.vo.PetLostDetailVO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pet-lost")
public class PetLostController {

    private final PetLostService petLostService;

    public PetLostController(PetLostService petLostService) {
        this.petLostService = petLostService;
    }

    // 发布宠物走失信息
    @PostMapping
    public ResponseEntity<Result<PetLostDetailVO>> createLostPet(
            @Valid @RequestBody PetLostCreateDTO dto,
            HttpServletRequest request) {

        Long currentUserId =
                (Long) request.getAttribute("currentUserId");

        if (currentUserId == null) {
            throw new InvalidCredentialsException("请先登录");
        }

        PetLostDetailVO result =
                petLostService.createLostPet(dto, currentUserId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Result.success(result));
    }

    // 根据 ID 查询走失详情
    @GetMapping("/{id}")
    public Result<PetLostDetailVO> getLostPetById(
            @PathVariable Long id) {

        PetLostDetailVO result =
                petLostService.getLostPetById(id);

        return Result.success(result);
    }
}
