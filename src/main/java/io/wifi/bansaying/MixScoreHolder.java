package io.wifi.bansaying;

import net.minecraft.scoreboard.ScoreHolder;

public class MixScoreHolder implements ScoreHolder {

    String target;

    public void setTarget(String target) {
        this.target = target;
    }

    public String getTarget() {
        return this.target;
    }

    public MixScoreHolder(String target) {
        this.target = target;
    }

    @Override
    public String getNameForScoreboard() {
        // TODO Auto-generated method stub
        return this.target;
    }

}
