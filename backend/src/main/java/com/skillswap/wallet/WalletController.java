package com.skillswap.wallet;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skillswap.common.api.ApiResponse;
import com.skillswap.common.api.PageResponse;
import com.skillswap.security.UserPrincipal;
import com.skillswap.wallet.dto.WalletDtos.TransactionDto;
import com.skillswap.wallet.dto.WalletDtos.WalletDto;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

	private final WalletService walletService;

	public WalletController(WalletService walletService) {
		this.walletService = walletService;
	}

	@GetMapping("/balance")
	public ApiResponse<WalletDto> balance(@AuthenticationPrincipal UserPrincipal me) {
		return ApiResponse.ok(walletService.summary(me.getId()));
	}

	@GetMapping("/transactions")
	public ApiResponse<PageResponse<TransactionDto>> transactions(@AuthenticationPrincipal UserPrincipal me,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return ApiResponse.ok(walletService.transactions(me.getId(), page, size));
	}
}
