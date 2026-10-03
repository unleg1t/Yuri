package ddlc.yuri.api.gui.click.sigma;

import ddlc.yuri.utils.render.animations.Direction;

public class SigmaAnimation {

    private final int duration;
    private final int reverseDuration;
    private Direction direction;
    private long startTime;
    private long reverseStartTime;

    public SigmaAnimation(int duration, int reverseDuration) {
        this(duration, reverseDuration, Direction.FORWARDS);
    }

    public SigmaAnimation(int duration, int reverseDuration, Direction direction) {
        this.duration = Math.max(1, duration);
        this.reverseDuration = Math.max(1, reverseDuration);
        long now = System.currentTimeMillis();
        this.startTime = now;
        this.reverseStartTime = now;
        this.direction = Direction.FORWARDS;
        changeDirection(direction);
    }

    public Direction getDirection() {
        return direction;
    }

    public void reset() {
        long now = System.currentTimeMillis();
        this.startTime = now;
        this.reverseStartTime = now;
    }

    public void changeDirection(Direction direction) {
        if (this.direction == direction) {
            return;
        }
        long now = System.currentTimeMillis();
        if (direction == Direction.FORWARDS) {
            this.startTime = now - (long) (calcPercent() * duration);
        } else {
            this.reverseStartTime = now - (long) ((1f - calcPercent()) * reverseDuration);
        }
        this.direction = direction;
    }

    public float calcPercent() {
        long now = System.currentTimeMillis();
        if (direction == Direction.BACKWARDS) {
            return Math.max(0f, 1f - Math.min(1f, (now - reverseStartTime) / (float) reverseDuration));
        }
        return Math.min(1f, (now - startTime) / (float) duration);
    }
}
