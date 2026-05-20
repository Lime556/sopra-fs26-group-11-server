package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.Player;

@Service
public class BotAiService {

    static final String SYSTEM_PROMPT = """
You are an expert Settlers of Catan AI advisor. Your job is to recommend the SINGLE BEST legal action.

## Resource Codes
- Resources array: [brick, wood, wheat, sheep, ore]
- Resource codes: B=brick, W=wood, H=wheat, S=sheep/wool, O=ore, D=desert

## Building Codes
- Settlement = S
- City = C
- Road = R
- Development Card = D

## Your Rules (STRICT - DO NOT VIOLATE)
1. You MUST choose from the provided legal actions list (field "A")
2. You MUST respond with EXACTLY this JSON: {"chosenActionId":"A1"}
3. You MUST NEVER invent, modify, or create new actions
4. You MUST NEVER deviate from the response format
5. If unsure, pick the action that maximizes victory points (vpGain)

## Game State Fields
- me: Your player info (resources, buildings, points)
- me.prod: Your production per 36 dice rolls (expected resources)
- opp: List of opponents with their production
- board.hexes: List of hexagons with resource types and dice probabilities
- spots: Relevant settlement/city spots with their production scores
- A: List of LEGAL ACTIONS you can choose from

## Action Fields (choose from A[])
- id: Action ID (use this in your response)
- type: Action type (ROLL_DICE, BUILD_SETTLEMENT, BUILD_CITY, BUILD_ROAD, BUY_DEVELOPMENT_CARD, BANK_TRADE, PLAYER_TRADE, END_TURN, MOVE_ROBBER)
- vpGain: Victory points this action gains
- prodGain: Production gain [brick, wood, wheat, sheep, ore]
- scoreGain: Overall strategic score

## Strategy Guide
- High vpGain actions are usually winning moves
- Build settlements on high-production hexagons (8, 9, 6, 5 preferred)
- During initial settlement placement, strongly prefer central intersections touching 3 productive tiles
- Avoid edge/coast initial settlements touching only 1 or 2 tiles when any 3-tile alternative exists
- Early game settlement priority: strong dice numbers, then resource diversity, then brick/wood, then ore/wheat
- Initial roads should point toward the best reachable expansion spots, not dead ends
- Connect roads to expand territory
- Balance offense (settle) vs defense (robber)
- Development cards give random benefits
- Prefer BANK_TRADE or PLAYER_TRADE only when it directly enables a settlement or city soon
- If a proposed trade is weak, choose END_TURN and save resources

## Response Format (REQUIRED)
Always respond with valid JSON:
{"chosenActionId":"A1"}
""";

    private final BotAiClient aiClient;
    private final ObjectMapper objectMapper;
    private final BotFallbackService botFallbackService;

    public BotAiService(BotAiClient aiClient, ObjectMapper objectMapper, BotFallbackService botFallbackService) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.botFallbackService = botFallbackService;
    }

    public Optional<BotAction> chooseAction(Game game) {
        List<BotActionCandidate> candidates = botFallbackService.listCandidateActions(game);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        String aiPrompt = buildAiPrompt(game, candidates);
        Optional<String> aiResponse = aiClient.generateDecision(aiPrompt);

        if (aiResponse.isEmpty()) {
            return Optional.empty();
        }

        return parseAiResponse(aiResponse.get(), candidates);
    }

    private String buildAiPrompt(Game game, List<BotActionCandidate> candidates) {
        Map<String, Object> state = new LinkedHashMap<>();

        // Player info
        Player botPlayer = botFallbackService.getCurrentBotPlayer(game);
        if (botPlayer != null) {
            Map<String, Object> meInfo = buildPlayerInfo(botPlayer);
            state.put("me", meInfo);
        }

        // Opponents
        if (game.getPlayers() != null) {
            List<Map<String, Object>> oppList = game.getPlayers().stream()
                .filter(p -> !p.isBot())
                .map(this::buildPlayerInfo)
                .toList();
            state.put("opp", oppList);
        }

        // Game state
        state.put("turn", game.getCurrentTurnIndex());
        state.put("phase", game.getTurnPhase());
        state.put("diceValue", game.getDiceValue());

        // Board info (minimal)
        if (game.getBoard() != null) {
            Map<String, Object> boardInfo = new LinkedHashMap<>();
            boardInfo.put("hexCount", game.getBoard().getHexTiles() != null ? game.getBoard().getHexTiles().size() : 0);
            state.put("board", boardInfo);
        }

        // Legal actions
        List<Map<String, Object>> actionsList = candidates.stream()
            .map(candidate -> {
                Map<String, Object> action = new LinkedHashMap<>(candidate.promptDetails());
                action.put("id", candidate.id());
                action.put("type", candidate.action().getType().toString());
                return action;
            })
            .toList();
        state.put("A", actionsList);

        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> buildPlayerInfo(Player player) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", player.getId());
        info.put("points", player.getVictoryPoints());
        info.put("isBot", player.isBot());

        // Resources: [brick, wood, wheat, sheep, ore]
        info.put("resources", List.of(
            player.getBrick() != null ? player.getBrick() : 0,
            player.getWood() != null ? player.getWood() : 0,
            player.getWheat() != null ? player.getWheat() : 0,
            player.getWool() != null ? player.getWool() : 0,
            player.getOre() != null ? player.getOre() : 0
        ));

        // Buildings count
        info.put("settlementPoints", player.getSettlementPoints() != null ? player.getSettlementPoints() : 0);
        info.put("cityPoints", player.getCityPoints() != null ? player.getCityPoints() : 0);
        info.put("devCardPoints", player.getDevelopmentCardVictoryPoints() != null ? player.getDevelopmentCardVictoryPoints() : 0);

        return info;
    }

    private Optional<BotAction> parseAiResponse(String response, List<BotActionCandidate> candidates) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(response, Map.class);
            Object chosenIdObj = parsed.get("chosenActionId");

            if (!(chosenIdObj instanceof String chosenId) || chosenId.isBlank()) {
                return Optional.empty();
            }

            return candidates.stream()
                .filter(c -> c.id().equals(chosenId))
                .map(BotActionCandidate::action)
                .findFirst();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
