package com.pawfind.backend.controller;

import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.dto.PetProfileDTO;
import com.pawfind.backend.entity.*;
import com.pawfind.backend.service.PersonalCenterService;
import com.pawfind.backend.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PersonalCenterController {
    private final PersonalCenterService service;

    public PersonalCenterController(PersonalCenterService service) {
        this.service = service;
    }

    @GetMapping("/users/me/summary")
    public Result<PersonalCenterService.Summary> summary(@RequestAttribute Long currentUserId) {
        return Result.success(service.summary(currentUserId));
    }

    @GetMapping("/pets")
    public Result<PageVO<PetProfile>> pets(@RequestAttribute Long currentUserId,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(service.pets(currentUserId, page, size));
    }

    @PostMapping("/pets")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<PetProfile> createPet(@RequestAttribute Long currentUserId,
                                       @Valid @RequestBody PetProfileDTO dto) {
        return Result.success(service.savePet(null, currentUserId, dto));
    }

    @PutMapping("/pets/{id}")
    public Result<PetProfile> updatePet(@PathVariable Long id, @RequestAttribute Long currentUserId,
                                       @Valid @RequestBody PetProfileDTO dto) {
        return Result.success(service.savePet(id, currentUserId, dto));
    }

    @DeleteMapping("/pets/{id}")
    public Result<Void> deletePet(@PathVariable Long id, @RequestAttribute Long currentUserId) {
        service.deletePet(id, currentUserId);
        return Result.success(null);
    }

    @GetMapping("/pet-lost/mine")
    public Result<PageVO<PetLost>> lost(@RequestAttribute Long currentUserId,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return Result.success(service.lost(currentUserId, page, size));
    }

    @GetMapping("/pet-found/mine")
    public Result<PageVO<PetFound>> found(@RequestAttribute Long currentUserId,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return Result.success(service.found(currentUserId, page, size));
    }
}
