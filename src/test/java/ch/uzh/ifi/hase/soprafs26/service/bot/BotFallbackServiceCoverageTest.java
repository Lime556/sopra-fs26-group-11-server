package ch.uzh.ifi.hase.soprafs26.service.bot;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.uzh.ifi.hase.soprafs26.entity.Board;
import ch.uzh.ifi.hase.soprafs26.entity.Boat;
import ch.uzh.ifi.hase.soprafs26.entity.City;
import ch.uzh.ifi.hase.soprafs26.entity.Edge;
import ch.uzh.ifi.hase.soprafs26.entity.Game;
import ch.uzh.ifi.hase.soprafs26.entity.GamePhase;
import ch.uzh.ifi.hase.soprafs26.entity.Intersection;
import ch.uzh.ifi.hase.soprafs26.entity.Player;
import ch.uzh.ifi.hase.soprafs26.entity.Road;
import ch.uzh.ifi.hase.soprafs26.entity.Settlement;
import ch.uzh.ifi.hase.soprafs26.entity.TurnPhase;

class BotFallbackServiceCoverageTest {

    private BotFallbackService service;

    @BeforeEach
    void setup() {
        service = new BotFallbackService();
    }

    @Test
    void getCurrentBotPlayer_handlesNullsAndBounds() {
        assertNull(service.getCurrentBotPlayer(null));

        Game game = new Game();
        assertNull(service.getCurrentBotPlayer(game));

        game.setPlayers(List.of(player(1L, false)));
        game.setCurrentTurnIndex(-1);
        assertNull(service.getCurrentBotPlayer(game));

        game.setCurrentTurnIndex(1);
        assertNull(service.getCurrentBotPlayer(game));

        game.setCurrentTurnIndex(0);
        assertNull(service.getCurrentBotPlayer(game));

        Player bot = player(2L, true);
        game.setPlayers(List.of(bot));
        assertEquals(bot, service.getCurrentBotPlayer(game));
    }

    @Test
    void listCandidateActions_coversSetupRollDiscardAndSeven() {
        Game setup = gameWithSimpleBoard();
        Player bot = player(10L, true);
        setup.setPlayers(List.of(bot));
        setup.setCurrentTurnIndex(0);
        setup.setGamePhase(GamePhase.SETUP);

        List<BotActionCandidate> setupCandidates = service.listCandidateActions(setup);
        assertFalse(setupCandidates.isEmpty());

        Game roll = gameWithSimpleBoard();
        roll.setPlayers(List.of(bot));
        roll.setCurrentTurnIndex(0);
        roll.setGamePhase(GamePhase.ACTIVE);
        roll.setTurnPhase(TurnPhase.ROLL_DICE);
        List<BotActionCandidate> rollCandidates = service.listCandidateActions(roll);
        assertEquals(1, rollCandidates.size());
        assertEquals(BotActionType.ROLL_DICE, rollCandidates.get(0).action().getType());

        Game discard = gameWithSimpleBoard();
        discard.setPlayers(List.of(bot));
        discard.setCurrentTurnIndex(0);
        discard.setGamePhase(GamePhase.ACTIVE);
        discard.setTurnPhase(TurnPhase.DISCARD);
        List<BotActionCandidate> discardCandidates = service.listCandidateActions(discard);
        assertTrue(discardCandidates.isEmpty());

        Game seven = gameWithSimpleBoard();
        seven.setPlayers(List.of(bot));
        seven.setCurrentTurnIndex(0);
        seven.setGamePhase(GamePhase.ACTIVE);
        seven.setTurnPhase(TurnPhase.ACTION);
        seven.setDiceValue(7);
        seven.setRobberMovedAfterSevenRoll(false);
        List<BotActionCandidate> sevenCandidates = service.listCandidateActions(seven);
        assertTrue(sevenCandidates.stream().anyMatch(c -> c.action().getType() == BotActionType.MOVE_ROBBER));
    }

    @Test
    void chooseFallbackAction_handlesBotMissingAndDiscard() {
        Game game = gameWithSimpleBoard();
        game.setPlayers(List.of(player(null, true)));
        game.setCurrentTurnIndex(0);

        BotAction none = service.chooseFallbackAction(game);
        assertEquals(BotActionType.NONE, none.getType());

        Player bot = player(3L, true);
        game.setPlayers(List.of(bot));
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.DISCARD);

        BotAction discardAction = service.chooseFallbackAction(game);
        assertEquals(BotActionType.NONE, discardAction.getType());
        assertEquals(bot.getId(), discardAction.getPlayerId());
    }

    @Test
    void privateScoringAndUtilityMethods_coverBranches() {
        assertEquals(0, (int) invokePrivate(service, "diceWeight", new Class<?>[] {Integer.class}, (Object) null));
        assertEquals(0, (int) invokePrivate(service, "diceWeight", new Class<?>[] {Integer.class}, -1));
        assertTrue((int) invokePrivate(service, "diceWeight", new Class<?>[] {Integer.class}, 6) > 0);

        assertEquals("D", invokePrivate(service, "resourceCode", new Class<?>[] {String.class}, (Object) null));
        assertEquals("B", invokePrivate(service, "resourceCode", new Class<?>[] {String.class}, "brick"));
        assertEquals("W", invokePrivate(service, "resourceCode", new Class<?>[] {String.class}, "WOOD"));
        assertEquals("H", invokePrivate(service, "resourceCode", new Class<?>[] {String.class}, "wheat"));
        assertEquals("S", invokePrivate(service, "resourceCode", new Class<?>[] {String.class}, "wool"));
        assertEquals("O", invokePrivate(service, "resourceCode", new Class<?>[] {String.class}, "ore"));
        assertEquals("D", invokePrivate(service, "resourceCode", new Class<?>[] {String.class}, "unknown"));

        assertEquals(0, (int) invokePrivate(service, "safeSize", new Class<?>[] {List.class}, (Object) null));
        assertEquals(2, (int) invokePrivate(service, "safeSize", new Class<?>[] {List.class}, List.of(1, 2)));
        assertEquals(0, (int) invokePrivate(service, "resourceValue", new Class<?>[] {Integer.class}, (Object) null));
        assertEquals(5, (int) invokePrivate(service, "resourceValue", new Class<?>[] {Integer.class}, 5));
    }

    @Test
    void buildAdjacentHexSummary_andBuildProductionGain_coverDataBranches() {
        Game game = new Game();
        game.setRobberTileIndex(2);
        Board board = new Board();
        board.setIntersections(List.of(intersection(7)));
        board.setEdges(List.of());
        board.setHexTiles(Arrays.asList("WOOD", "DESERT", "ORE", null));
        board.setHexTile_DiceNumbers(Arrays.asList(8, 6, 5, null));
        game.setBoard(board);

        List<List<Object>> summary = invokePrivate(service, "buildAdjacentHexSummary",
            new Class<?>[] {Game.class, Integer.class}, game, 7);
        assertNotNull(summary);

        List<Integer> gain = invokePrivate(service, "buildProductionGain",
            new Class<?>[] {Game.class, Integer.class}, game, 7);
        assertEquals(5, gain.size());

        int score = invokePrivate(service, "productionScoreForIntersection",
            new Class<?>[] {Game.class, Integer.class}, game, 7);
        assertTrue(score >= 0);
    }

    @Test
    void findIntersectionAndAdjacency_andOwnership_checksBranches() {
        Game game = gameWithSimpleBoard();
        Player bot = player(11L, true);
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);

        Intersection i1 = game.getBoard().getIntersections().get(0);
        Intersection i2 = game.getBoard().getIntersections().get(1);
        i2.setBuilding(settlement(bot.getId(), i2.getId()));

        Intersection found = invokePrivate(service, "findIntersectionById",
            new Class<?>[] {Game.class, Integer.class}, game, i1.getId());
        assertEquals(i1.getId(), found.getId());

        boolean adjacent = invokePrivate(service, "hasAdjacentBuilding",
            new Class<?>[] {Game.class, Integer.class}, game, i1.getId());
        assertTrue(adjacent);

        boolean ownBuilding = invokePrivate(service, "hasOwnBuildingAtIntersection",
            new Class<?>[] {Game.class, Integer.class, Long.class}, game, i2.getId(), bot.getId());
        assertTrue(ownBuilding);

        game.getBoard().getEdges().get(0).setRoad(road(bot.getId(), game.getBoard().getEdges().get(0).getId()));
        boolean ownRoad = invokePrivate(service, "hasOwnRoadAtIntersection",
            new Class<?>[] {Game.class, Integer.class, Long.class}, game, i1.getId(), bot.getId());
        assertTrue(ownRoad);

        Boolean nullIntersectionRoad = invokePrivate(service, "hasOwnRoadAtIntersection",
            new Class<?>[] {Game.class, Integer.class, Long.class}, game, null, bot.getId());
        Boolean nullPlayerRoad = invokePrivate(service, "hasOwnRoadAtIntersection",
            new Class<?>[] {Game.class, Integer.class, Long.class}, game, i1.getId(), null);
        assertFalse(Boolean.TRUE.equals(nullIntersectionRoad));
        assertFalse(Boolean.TRUE.equals(nullPlayerRoad));
    }

    @Test
    void addCandidate_andRandomCandidate_coverNullAndNonNull() {
        List<BotActionCandidate> candidates = new ArrayList<>();

        invokePrivate(service, "addCandidate",
            new Class<?>[] {List.class, BotAction.class, Map.class}, candidates, null, Map.of());
        assertTrue(candidates.isEmpty());

        invokePrivate(service, "addCandidate",
            new Class<?>[] {List.class, BotAction.class, Map.class}, candidates,
            BotAction.of(BotActionType.END_TURN, 1L), Map.of("t", "END_TURN"));
        assertEquals(1, candidates.size());
        assertEquals("A1", candidates.get(0).id());

        assertNull(invokePrivate(service, "randomCandidate", new Class<?>[] {List.class}, (Object) null));
        assertNull(invokePrivate(service, "randomCandidate", new Class<?>[] {List.class}, List.of()));
        assertEquals(7, (int) invokePrivate(service, "randomCandidate", new Class<?>[] {List.class}, List.of(7)));
    }

    @Test
    void chooseMainAction_andSetupAction_coverDecisionBranches() {
        Game game = gameWithSimpleBoard();
        Player bot = player(20L, true);
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);

        game.setGamePhase(GamePhase.SETUP);
        game.getBoard().getIntersections().get(0).setBuilding(settlement(bot.getId(), 1));
        bot.setLastPlacedSetupSettlementIntersectionId(1);
        BotAction setupAction = invokePrivate(service, "chooseSetupAction",
            new Class<?>[] {Game.class, Player.class}, game, bot);
        assertTrue(setupAction.getType() == BotActionType.BUILD_INITIAL_SETTLEMENT
            || setupAction.getType() == BotActionType.BUILD_INITIAL_ROAD
            || setupAction.getType() == BotActionType.END_TURN);

        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.ACTION);
        bot.setWheat(2);
        bot.setOre(3);
        BotAction mainAction = invokePrivate(service, "chooseMainAction",
            new Class<?>[] {Game.class, Player.class}, game, bot);
        assertNotNull(mainAction);
    }

    @Test
    void canBuyDevelopmentCard_andFirstValidRobberHex_coverBranches() {
        Game game = gameWithSimpleBoard();
        Player bot = player(30L, true);

        game.setDevelopmentKnightRemaining(0);
        game.setDevelopmentVictoryPointRemaining(0);
        game.setDevelopmentRoadBuildingRemaining(0);
        game.setDevelopmentYearOfPlentyRemaining(0);
        game.setDevelopmentMonopolyRemaining(0);
        bot.setWool(1);
        bot.setWheat(1);
        bot.setOre(1);

        boolean cannotBuy = invokePrivate(service, "canBuyDevelopmentCard",
            new Class<?>[] {Game.class, Player.class}, game, bot);
        assertFalse(cannotBuy);

        game.setDevelopmentKnightRemaining(1);
        boolean canBuy = invokePrivate(service, "canBuyDevelopmentCard",
            new Class<?>[] {Game.class, Player.class}, game, bot);
        assertTrue(canBuy);

        game.setBoard(null);
        assertNull(invokePrivate(service, "firstValidRobberHex", new Class<?>[] {Game.class}, game));

        game.setBoard(gameWithSimpleBoard().getBoard());
        game.setRobberTileIndex(1);
        Integer robberHex = invokePrivate(service, "firstValidRobberHex", new Class<?>[] {Game.class}, game);
        assertTrue(robberHex == null || robberHex > 1);
    }

    @Test
    void intersectionsAndEdges_helpersReturnEmptyForNullBoard() {
        Game game = new Game();
        List<?> intersections = invokePrivate(service, "intersections", new Class<?>[] {Game.class}, game);
        List<?> edges = invokePrivate(service, "edges", new Class<?>[] {Game.class}, game);
        assertTrue(intersections.isEmpty());
        assertTrue(edges.isEmpty());
    }

    @Test
    void countMethods_coverSettlementsCitiesRoads() {
        Game game = gameWithSimpleBoard();
        Long playerId = 77L;

        game.getBoard().getIntersections().get(0).setBuilding(settlement(playerId, 1));
        City city = new City();
        city.setOwnerPlayerId(playerId);
        game.getBoard().getIntersections().get(1).setBuilding(city);
        game.getBoard().getEdges().get(0).setRoad(road(playerId, game.getBoard().getEdges().get(0).getId()));

        int settlements = invokePrivate(service, "countPlayerSettlements", new Class<?>[] {Game.class, Long.class}, game, playerId);
        int cities = invokePrivate(service, "countPlayerCities", new Class<?>[] {Game.class, Long.class}, game, playerId);
        int roads = invokePrivate(service, "countPlayerRoads", new Class<?>[] {Game.class, Long.class}, game, playerId);

        assertEquals(1, settlements);
        assertEquals(1, cities);
        assertEquals(1, roads);
    }

    @Test
    void tradeResourceAndPortHelpers_coverSwitchAndNullBranches() {
        assertFalse((Boolean) invokePrivate(service, "boatTypeMatchesPortType",
            new Class<?>[] {String.class, String.class}, null, "wood"));
        assertFalse((Boolean) invokePrivate(service, "boatTypeMatchesPortType",
            new Class<?>[] {String.class, String.class}, "WOOD", null));
        assertTrue((Boolean) invokePrivate(service, "boatTypeMatchesPortType",
            new Class<?>[] {String.class, String.class}, "STANDARD", "3:1"));
        assertTrue((Boolean) invokePrivate(service, "boatTypeMatchesPortType",
            new Class<?>[] {String.class, String.class}, "WOOD", "wood"));
        assertTrue((Boolean) invokePrivate(service, "boatTypeMatchesPortType",
            new Class<?>[] {String.class, String.class}, "BRICK", "brick"));
        assertTrue((Boolean) invokePrivate(service, "boatTypeMatchesPortType",
            new Class<?>[] {String.class, String.class}, "SHEEP", "wool"));
        assertTrue((Boolean) invokePrivate(service, "boatTypeMatchesPortType",
            new Class<?>[] {String.class, String.class}, "WHEAT", "wheat"));
        assertTrue((Boolean) invokePrivate(service, "boatTypeMatchesPortType",
            new Class<?>[] {String.class, String.class}, "STONE", "ore"));
        assertFalse((Boolean) invokePrivate(service, "boatTypeMatchesPortType",
            new Class<?>[] {String.class, String.class}, "UNKNOWN", "ore"));

        Game game = new Game();
        game.setBankWood(1);
        game.setBankBrick(2);
        game.setBankWool(3);
        game.setBankWheat(4);
        game.setBankOre(5);
        assertEquals(0, (int) invokePrivate(service, "bankResourceValue",
            new Class<?>[] {Game.class, String.class}, null, "wood"));
        assertEquals(0, (int) invokePrivate(service, "bankResourceValue",
            new Class<?>[] {Game.class, String.class}, game, null));
        assertEquals(1, (int) invokePrivate(service, "bankResourceValue",
            new Class<?>[] {Game.class, String.class}, game, "wood"));
        assertEquals(2, (int) invokePrivate(service, "bankResourceValue",
            new Class<?>[] {Game.class, String.class}, game, "brick"));
        assertEquals(3, (int) invokePrivate(service, "bankResourceValue",
            new Class<?>[] {Game.class, String.class}, game, "wool"));
        assertEquals(4, (int) invokePrivate(service, "bankResourceValue",
            new Class<?>[] {Game.class, String.class}, game, "wheat"));
        assertEquals(5, (int) invokePrivate(service, "bankResourceValue",
            new Class<?>[] {Game.class, String.class}, game, "ore"));
        assertEquals(0, (int) invokePrivate(service, "bankResourceValue",
            new Class<?>[] {Game.class, String.class}, game, "gold"));

        Player player = player(90L, true);
        player.setWood(6);
        player.setBrick(7);
        player.setWool(8);
        player.setWheat(9);
        player.setOre(10);
        assertEquals(0, (int) invokePrivate(service, "resourceValueForName",
            new Class<?>[] {Player.class, String.class}, null, "wood"));
        assertEquals(0, (int) invokePrivate(service, "resourceValueForName",
            new Class<?>[] {Player.class, String.class}, player, null));
        assertEquals(6, (int) invokePrivate(service, "resourceValueForName",
            new Class<?>[] {Player.class, String.class}, player, "wood"));
        assertEquals(7, (int) invokePrivate(service, "resourceValueForName",
            new Class<?>[] {Player.class, String.class}, player, "brick"));
        assertEquals(8, (int) invokePrivate(service, "resourceValueForName",
            new Class<?>[] {Player.class, String.class}, player, "wool"));
        assertEquals(9, (int) invokePrivate(service, "resourceValueForName",
            new Class<?>[] {Player.class, String.class}, player, "wheat"));
        assertEquals(10, (int) invokePrivate(service, "resourceValueForName",
            new Class<?>[] {Player.class, String.class}, player, "ore"));
        assertEquals(0, (int) invokePrivate(service, "resourceValueForName",
            new Class<?>[] {Player.class, String.class}, player, "gold"));
    }

    @Test
    void portHelpers_coverSpecificGenericInvalidAndDefaultRatios() {
        Game noPortGame = gameWithSimpleBoard();
        Player player = player(91L, true);

        assertEquals(4, (int) invokePrivate(service, "bestBankRatio",
            new Class<?>[] {Game.class, Player.class, String.class}, noPortGame, player, "wood"));
        assertFalse((Boolean) invokePrivate(service, "hasPortAccess",
            new Class<?>[] {Game.class, Player.class, String.class}, noPortGame, player, null));
        assertFalse((Boolean) invokePrivate(service, "isIntersectionAPortOfType",
            new Class<?>[] {Board.class, Integer.class, String.class}, null, 1, "wood"));

        Game portGame = new Game();
        Board board = new Board();
        board.generateBoard();
        portGame.setBoard(board);
        player.setId(91L);

        Boat woodPort = board.getBoats().stream()
            .filter(boat -> "WOOD".equals(boat.getBoatType()))
            .findFirst()
            .orElseThrow();
        Integer woodPortIntersection = board.getIntersectionIdsForHex(woodPort.getHexId()).get(woodPort.getFirstCorner());
        board.getIntersections().get(woodPortIntersection).setBuilding(settlement(player.getId(), woodPortIntersection));
        assertTrue((Boolean) invokePrivate(service, "hasPortAccess",
            new Class<?>[] {Game.class, Player.class, String.class}, portGame, player, "wood"));
        assertEquals(2, (int) invokePrivate(service, "bestBankRatio",
            new Class<?>[] {Game.class, Player.class, String.class}, portGame, player, "wood"));

        Player genericPortPlayer = player(92L, true);
        Boat standardPort = board.getBoats().stream()
            .filter(boat -> "STANDARD".equals(boat.getBoatType()))
            .findFirst()
            .orElseThrow();
        Integer standardPortIntersection = board.getIntersectionIdsForHex(standardPort.getHexId()).get(standardPort.getFirstCorner());
        board.getIntersections().get(standardPortIntersection).setBuilding(settlement(genericPortPlayer.getId(), standardPortIntersection));
        assertEquals(3, (int) invokePrivate(service, "bestBankRatio",
            new Class<?>[] {Game.class, Player.class, String.class}, portGame, genericPortPlayer, "ore"));

        Board invalidBoatBoard = new Board();
        invalidBoatBoard.generateBoard();
        Boat invalidBoat = new Boat();
        invalidBoat.setHexId(1);
        invalidBoat.setFirstCorner(99);
        invalidBoat.setSecondCorner(100);
        invalidBoat.setBoatType("WOOD");
        invalidBoatBoard.setBoats(Arrays.asList(null, invalidBoat));
        assertFalse((Boolean) invokePrivate(service, "isIntersectionAPortOfType",
            new Class<?>[] {Board.class, Integer.class, String.class}, invalidBoatBoard, 1, "wood"));
    }

    @Test
    void savingTradeAndDevelopmentHelpers_coverDecisionBranches() {
        Player missingOneSettlementResource = player(100L, true);
        missingOneSettlementResource.setWood(1);
        missingOneSettlementResource.setBrick(1);
        missingOneSettlementResource.setWool(0);
        missingOneSettlementResource.setWheat(1);
        missingOneSettlementResource.setOre(0);
        assertTrue((Boolean) invokePrivate(service, "isSavingForSettlement",
            new Class<?>[] {Player.class}, missingOneSettlementResource));
        assertEquals("settlement", invokePrivate(service, "tradeEnablesPlan",
            new Class<?>[] {Player.class, String.class}, missingOneSettlementResource, "wool"));

        Player missingOneCityResource = player(101L, true);
        missingOneCityResource.setWood(1);
        missingOneCityResource.setBrick(1);
        missingOneCityResource.setWool(1);
        missingOneCityResource.setWheat(2);
        missingOneCityResource.setOre(2);
        assertTrue((Boolean) invokePrivate(service, "isSavingForCity",
            new Class<?>[] {Player.class}, missingOneCityResource));
        assertEquals("city", invokePrivate(service, "tradeEnablesPlan",
            new Class<?>[] {Player.class, String.class}, missingOneCityResource, "ore"));

        Player farFromBuild = player(102L, true);
        farFromBuild.setWood(0);
        farFromBuild.setBrick(0);
        farFromBuild.setWool(1);
        farFromBuild.setWheat(1);
        farFromBuild.setOre(1);
        assertFalse((Boolean) invokePrivate(service, "isSavingForSettlement",
            new Class<?>[] {Player.class}, farFromBuild));
        assertFalse((Boolean) invokePrivate(service, "isSavingForCity",
            new Class<?>[] {Player.class}, farFromBuild));
        assertEquals("build", invokePrivate(service, "tradeEnablesPlan",
            new Class<?>[] {Player.class, String.class}, farFromBuild, null));

        Game devCardGame = gameWithSimpleBoard();
        devCardGame.setDevelopmentKnightRemaining(1);
        devCardGame.getBoard().getIntersections().get(0).setBuilding(settlement(farFromBuild.getId(), 1));
        devCardGame.getBoard().getIntersections().get(2).setBuilding(settlement(farFromBuild.getId(), 3));
        City city = new City();
        city.setOwnerPlayerId(farFromBuild.getId());
        devCardGame.getBoard().getIntersections().get(3).setBuilding(city);

        assertTrue((Boolean) invokePrivate(service, "shouldBuyDevelopmentCard",
            new Class<?>[] {Game.class, Player.class}, devCardGame, farFromBuild));
        assertFalse((Boolean) invokePrivate(service, "shouldBuyDevelopmentCard",
            new Class<?>[] {Game.class, Player.class}, devCardGame, missingOneSettlementResource));
    }

    @Test
    void roadSettlementAndRobberHelpers_coverPositiveAndNegativeBranches() {
        Game game = new Game();
        Board board = new Board();
        board.generateBoard();
        game.setBoard(board);
        game.setRobberTileIndex(1);

        Player bot = player(110L, true);
        bot.setWood(1);
        bot.setBrick(1);
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);

        board.getEdges().get(0).setRoad(road(bot.getId(), board.getEdges().get(0).getId()));
        Integer roadTarget = board.getEdges().get(0).getIntersectionBId();

        assertTrue((Boolean) invokePrivate(service, "canBuildSettlementSpot",
            new Class<?>[] {Game.class, Player.class}, game, bot));
        assertTrue((Boolean) invokePrivate(service, "canEventuallyBuildSettlementAt",
            new Class<?>[] {Game.class, Integer.class}, game, roadTarget));
        assertFalse((Boolean) invokePrivate(service, "canEventuallyBuildSettlementAt",
            new Class<?>[] {Game.class, Integer.class}, game, null));
        assertTrue((int) invokePrivate(service, "roadExpansionScore",
            new Class<?>[] {Game.class, Edge.class}, game, board.getEdges().get(0)) >= 0);
        assertEquals(0, (int) invokePrivate(service, "roadExpansionScore",
            new Class<?>[] {Game.class, Edge.class}, game, null));
        assertTrue((Boolean) invokePrivate(service, "roadOpensSettlement",
            new Class<?>[] {Game.class, Edge.class}, game, board.getEdges().get(0)));
        assertFalse((Boolean) invokePrivate(service, "roadOpensSettlement",
            new Class<?>[] {Game.class, Edge.class}, game, null));

        Player opponent = player(111L, false);
        Integer threatenedIntersection = board.getIntersectionIdsForHex(2).get(0);
        board.getIntersections().get(threatenedIntersection).setBuilding(settlement(opponent.getId(), threatenedIntersection));
        assertTrue((int) invokePrivate(service, "robberThreatScore",
            new Class<?>[] {Game.class, int.class}, game, 2) > 0);
        assertEquals(0, (int) invokePrivate(service, "robberThreatScore",
            new Class<?>[] {Game.class, int.class}, new Game(), 2));
    }

    @Test
    void buildProductionGain_coversEveryResourceAndBlockedTiles() {
        for (String resource : List.of("BRICK", "WOOD", "WHEAT", "WOOL", "ORE")) {
            Game game = new Game();
            Board board = new Board();
            board.generateBoard();
            board.setHexTiles(new ArrayList<>(Collections.nCopies(19, resource)));
            board.setHexTile_DiceNumbers(new ArrayList<>(Collections.nCopies(19, 6)));
            game.setBoard(board);
            game.setRobberTileIndex(99);

            List<Integer> gain = invokePrivate(service, "buildProductionGain",
                new Class<?>[] {Game.class, Integer.class}, game, 0);
            assertEquals(5, gain.size());
            assertTrue(gain.stream().mapToInt(Integer::intValue).sum() > 0);
        }

        Game blocked = new Game();
        Board board = new Board();
        board.generateBoard();
        board.setHexTiles(new ArrayList<>(Collections.nCopies(19, "WOOD")));
        board.setHexTile_DiceNumbers(new ArrayList<>(Collections.nCopies(19, 6)));
        blocked.setBoard(board);
        blocked.setRobberTileIndex(1);

        List<Integer> blockedGain = invokePrivate(service, "buildProductionGain",
            new Class<?>[] {Game.class, Integer.class}, blocked, 0);
        assertTrue(blockedGain.stream().mapToInt(Integer::intValue).sum() >= 0);
    }

    @Test
    void addMainCandidates_includesSettlementRoadAndEndTurnBranches() {
        Game game = new Game();
        Board board = new Board();
        board.generateBoard();
        game.setBoard(board);
        game.setDiceValue(6);
        game.setRobberMovedAfterSevenRoll(true);

        Player bot = player(120L, true);
        bot.setWood(2);
        bot.setBrick(2);
        bot.setWool(1);
        bot.setWheat(1);
        bot.setOre(0);
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.ACTION);
        board.getEdges().get(0).setRoad(road(bot.getId(), board.getEdges().get(0).getId()));

        List<BotActionCandidate> candidates = service.listCandidateActions(game);

        assertTrue(candidates.stream().anyMatch(candidate -> candidate.action().getType() == BotActionType.BUILD_SETTLEMENT));
        assertTrue(candidates.stream().anyMatch(candidate -> candidate.action().getType() == BotActionType.BUILD_ROAD));
        assertTrue(candidates.stream().anyMatch(candidate -> candidate.action().getType() == BotActionType.END_TURN));
    }

    @Test
    void addMainCandidates_includesDevelopmentCardWhenBotIsNotSavingForBuild() {
        Game game = gameWithSimpleBoard();
        game.setDevelopmentKnightRemaining(1);
        Player bot = player(121L, true);
        bot.setWood(0);
        bot.setBrick(0);
        bot.setWool(1);
        bot.setWheat(1);
        bot.setOre(1);

        game.getBoard().getIntersections().get(0).setBuilding(settlement(bot.getId(), 1));
        game.getBoard().getIntersections().get(1).setBuilding(settlement(bot.getId(), 2));
        game.getBoard().getIntersections().get(2).setBuilding(settlement(bot.getId(), 3));
        game.setPlayers(List.of(bot));
        game.setCurrentTurnIndex(0);
        game.setGamePhase(GamePhase.ACTIVE);
        game.setTurnPhase(TurnPhase.ACTION);

        List<BotActionCandidate> candidates = service.listCandidateActions(game);

        assertTrue(candidates.stream().anyMatch(candidate -> candidate.action().getType() == BotActionType.BUY_DEVELOPMENT_CARD));
    }

    @Test
    void randomValidSettlement_coversInsufficientResourcesMaxAndValidRoad() {
        Game game = gameWithSimpleBoard();
        Player bot = player(122L, true);
        bot.setWood(0);
        assertNull(invokePrivate(service, "randomValidSettlement", new Class<?>[] {Game.class, Player.class}, game, bot));

        bot.setWood(1);
        bot.setBrick(1);
        bot.setWool(1);
        bot.setWheat(1);
        game.getBoard().setIntersections(List.of(
            intersection(1), intersection(2), intersection(3), intersection(4), intersection(5)
        ));
        for (int i = 0; i < 5; i++) {
            game.getBoard().getIntersections().get(i)
                .setBuilding(settlement(bot.getId(), i));
        }
        assertNull(invokePrivate(service, "randomValidSettlement", new Class<?>[] {Game.class, Player.class}, game, bot));

        Game validGame = gameWithSimpleBoard();
        validGame.getBoard().getEdges().get(0).setRoad(road(bot.getId(), 1));
        Integer settlementId = invokePrivate(service, "randomValidSettlement",
            new Class<?>[] {Game.class, Player.class}, validGame, bot);
        assertNotNull(settlementId);
    }

    @Test
    void bestValidRoad_coversResourceLimitRoadLimitAndFreeRoadBranches() {
        Game game = gameWithSimpleBoard();
        Player bot = player(123L, true);
        bot.setWood(0);
        bot.setBrick(0);
        assertNull(invokePrivate(service, "bestValidRoad", new Class<?>[] {Game.class, Player.class}, game, bot));

        bot.setWood(1);
        bot.setBrick(1);
        for (int i = 0; i < 15; i++) {
            Edge edge = game.getBoard().getEdges().get(i % game.getBoard().getEdges().size());
            edge.setRoad(road(bot.getId(), edge.getId()));
        }
        assertNull(invokePrivate(service, "bestValidRoad", new Class<?>[] {Game.class, Player.class}, game, bot));

        Game freeRoadGame = gameWithSimpleBoard();
        bot.setFreeRoadBuildsRemaining(1);
        bot.setWood(0);
        bot.setBrick(0);
        freeRoadGame.getBoard().getIntersections().get(0).setBuilding(settlement(bot.getId(), 1));
        Integer edgeId = invokePrivate(service, "bestValidRoad", new Class<?>[] {Game.class, Player.class}, freeRoadGame, bot);
        assertNotNull(edgeId);
    }

    private Game gameWithSimpleBoard() {
        Game game = new Game();
        Board board = new Board();
        board.setHexTiles(List.of("WOOD", "BRICK", "WHEAT"));
        board.setHexTile_DiceNumbers(List.of(8, 6, 4));
        board.setIntersections(List.of(intersection(1), intersection(2), intersection(3), intersection(4)));
        board.setEdges(List.of(edge(1, 1, 2), edge(2, 2, 3), edge(3, 3, 4)));
        game.setBoard(board);
        game.setRobberTileIndex(1);
        return game;
    }

    private Player player(Long id, boolean bot) {
        Player player = new Player();
        player.setId(id);
        player.setBot(bot);
        player.setWood(2);
        player.setBrick(2);
        player.setWool(2);
        player.setWheat(2);
        player.setOre(3);
        return player;
    }

    private Intersection intersection(int id) {
        Intersection i = new Intersection();
        i.setId(id);
        return i;
    }

    private Edge edge(int id, int a, int b) {
        Edge edge = new Edge();
        edge.setId(id);
        edge.setIntersectionAId(a);
        edge.setIntersectionBId(b);
        return edge;
    }

    private Settlement settlement(Long playerId, Integer intersectionId) {
        Settlement settlement = new Settlement();
        settlement.setOwnerPlayerId(playerId);
        settlement.setIntersectionId(intersectionId);
        return settlement;
    }

    private Road road(Long playerId, Integer edgeId) {
        Road road = new Road();
        road.setOwnerPlayerId(playerId);
        road.setEdgeId(edgeId);
        return road;
    }

    @SuppressWarnings("unchecked")
    private <T> T invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return (T) method.invoke(target, args);
        } catch (Exception exception) {
            throw new AssertionError("Failed to invoke private method " + methodName, exception);
        }
    }
}
