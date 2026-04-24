package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.Instant;

public class DiceRollDTO {
    private Integer diceValue;
    private Instant diceRolledAt;
    public Integer getDiceValue() { return diceValue; }
    public void setDiceValue(Integer diceValue) { this.diceValue = diceValue; }
    public Instant getDiceRolledAt() { return diceRolledAt; }
    public void setDiceRolledAt(Instant diceRolledAt) { this.diceRolledAt = diceRolledAt; }
}