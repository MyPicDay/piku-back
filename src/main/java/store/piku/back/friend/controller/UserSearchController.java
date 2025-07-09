package store.piku.back.friend.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import store.piku.back.friend.dto.FriendsDTO;
import org.springframework.data.domain.Pageable;
import store.piku.back.user.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/명칭 ")
@Slf4j
@RequiredArgsConstructor
public class UserSearchController {

    private final UserService userService;

    @GetMapping
    public Page<FriendsDTO> search(@ParameterObject @PageableDefault(size = 3)  Pageable pageable,
                                   @RequestParam String keyword) {

        return userService.searchByName(keyword, pageable);
    }


}
