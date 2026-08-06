package com.weddingraffle.rifa.service.impl;

import com.weddingraffle.rifa.dto.HomeSummaryResponse;
import com.weddingraffle.rifa.dto.RaffleConfigResponse;
import com.weddingraffle.rifa.dto.TopBuyerResponse;
import com.weddingraffle.rifa.repository.TopBuyerProjection;
import com.weddingraffle.rifa.repository.TransactionRepository;
import com.weddingraffle.rifa.service.HomeSummaryService;
import com.weddingraffle.rifa.service.RaffleConfigService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeSummaryServiceImpl implements HomeSummaryService {

    private static final List<String> AVATAR_EMOJIS = List.of(
            "🎁", "✨", "💍", "🌹", "🍀", "⭐", "🎉", "💛", "🥂", "🎶", "🌙", "☀️", "💐", "🎈", "💎", "🏆", "🕊️", "🍰",
            "📸", "💌", "🌻", "🔥", "🪩", "🎵", "🌟", "💫", "🧡", "💚", "💙", "🤍", "🍾", "🎊", "🌺", "🕯️", "👑", "🪷",
            "🧿", "🔔", "💒", "🎀");
    private static final List<String> AVATAR_COLORS =
            List.of("#B75D46", "#C6922E", "#6E7F52", "#7E6C9E", "#2F6F73", "#B85C83", "#8A6A00", "#4F6F9F");

    private final RaffleConfigService raffleConfigService;
    private final TransactionRepository transactionRepository;

    public HomeSummaryServiceImpl(
            RaffleConfigService raffleConfigService, TransactionRepository transactionRepository) {
        this.raffleConfigService = raffleConfigService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public HomeSummaryResponse getSummary() {
        RaffleConfigResponse config = raffleConfigService.getConfig();
        List<TopBuyerResponse> topBuyers = transactionRepository.findTopApprovedBuyers().stream()
                .map(this::toTopBuyer)
                .toList();
        return new HomeSummaryResponse(config.scheduledDrawAt(), topBuyers);
    }

    private TopBuyerResponse toTopBuyer(TopBuyerProjection projection) {
        String hash = hash(projection.getPhone());
        int emojiIndex = Math.floorMod(hash.substring(0, 8).hashCode(), AVATAR_EMOJIS.size());
        int colorIndex = Math.floorMod(hash.substring(8, 16).hashCode(), AVATAR_COLORS.size());
        return new TopBuyerResponse(
                AVATAR_EMOJIS.get(emojiIndex), AVATAR_COLORS.get(colorIndex), projection.getQuantity());
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
