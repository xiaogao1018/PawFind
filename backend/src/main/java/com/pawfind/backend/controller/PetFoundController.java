package com.pawfind.backend.controller;

import com.pawfind.backend.common.result.Result;
import com.pawfind.backend.dto.PetFoundDTO;
import com.pawfind.backend.entity.PetFound;
import com.pawfind.backend.service.PetFoundService;
import com.pawfind.backend.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pet-found")
public class PetFoundController {
    private final PetFoundService service;

    public PetFoundController(PetFoundService service) {
        this.service = service;
    }

    @GetMapping
    public Result<PageVO<PetFound>> list(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size) {
        return Result.success(service.list(page, size));
    }

    @GetMapping("/{id}")
    public Result<PetFound> get(@PathVariable long id) {
        return Result.success(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<PetFound> create(@Valid @RequestBody PetFoundDTO dto, @RequestAttribute Long currentUserId) {
        return Result.success(service.create(dto, currentUserId));
    }

    @PutMapping("/{id}")
    public Result<PetFound> update(@PathVariable long id, @Valid @RequestBody PetFoundDTO dto,
                                   @RequestAttribute Long currentUserId) {
        return Result.success(service.update(id, dto, currentUserId));
    }

    @DeleteMapping("/{id}")
    public Result<Void> withdraw(@PathVariable long id, @RequestAttribute Long currentUserId) {
        service.withdraw(id, currentUserId);
        return Result.success(null);
    }
}
