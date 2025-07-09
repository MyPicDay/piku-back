package store.piku.back.friend.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import store.piku.back.friend.dto.FriendsDTO;
import org.springframework.data.domain.Pageable;
import store.piku.back.global.dto.RequestMetaInfo;
import store.piku.back.global.util.RequestMetaMapper;
import store.piku.back.user.service.UserService;

@Tag(name = "Friends", description = "친구 검색 API")
@RestController
@RequestMapping("/api/search")
@Slf4j
@RequiredArgsConstructor
public class UserSearchController {

    private final UserService userService;
    private final RequestMetaMapper requestMetaMapper;

    @Operation(summary = "친구 이름으로 검색", description = "키워드를 이용해 친구 목록을 페이징 조회합니다.")
    @GetMapping
    public Page<FriendsDTO> search(@ParameterObject @PageableDefault(sort = "createdAt")  Pageable pageable,
                                   @RequestParam String keyword, HttpServletRequest request) {

        RequestMetaInfo requestMetaInfo = requestMetaMapper.extractMetaInfo(request);
        return userService.searchByName(keyword, pageable,requestMetaInfo);
    }


}
