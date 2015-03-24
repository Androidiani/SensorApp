package it.unina.is2project.sensorgames.pong;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.engine.handler.physics.PhysicsHandler;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.region.ITextureRegion;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import it.unina.is2project.sensorgames.R;

import static org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory.createFromAsset;
import static org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory.createFromResource;

public class GamePongOnePlayer extends GamePong {

    /*
        Scene
    */
    private boolean game_over = false;

    /*
        Graphics
    */
    // Text View
    private Text txtScore;
    private Text txtEvnt;
    private Text txtLvl;

    // Life
    private BitmapTextureAtlas lifeTexture;
    private ITextureRegion lifeTextureRegion;
    private List<Sprite> lifeSprites = new ArrayList<Sprite>();

    // First enemy
    private Sprite firstEnemy;

    // Rush Hour
    private List<Sprite> rushHour = new ArrayList<Sprite>();
    private List<PhysicsHandler> rushHourHandlers = new ArrayList<PhysicsHandler>();

    // Bonus ball
    private BitmapTextureAtlas bonusBallTexture;
    private ITextureRegion bonusBallTextureRegion;
    private List<Sprite> bonusBalls = new ArrayList<Sprite>();

    // Life bonus
    private Sprite lifeBonus;

    /*
        Game data
    */
    private int score = 0;
    private int gain;
    private static final int MAX_LIFE = 3;
    private int life = MAX_LIFE - 1;

    // Levels
    private int level;
    private boolean level_one = true;
    private static final int LEVEL_ONE = 0;
    private static final int BARRIER_ONE = 10;
    private boolean level_two = false;
    private static final int LEVEL_TWO = 1;
    private static final int BARRIER_TWO = 60;
    private boolean level_three = false;
    private static final int LEVEL_THREE = 2;
    private static final int BARRIER_THREE = 400;
    private boolean level_four = false;
    private static final int LEVEL_FOUR = 3;
    private static final int BARRIER_FOUR = 1000;
    private boolean level_five = false;
    private static final int LEVEL_FIVE = 4;
    private static final int BARRIER_FIVE = 3600;
    private boolean level_six = false;
    private static final int LEVEL_SIX = 5;
    private static final int BARRIER_SIX = 5200;
    private boolean level_seven = false;
    private static final int LEVEL_SEVEN = 6;
    private static final int BARRIER_SEVEN = 9200;

    /**
     * Events
     */
    private int game_event;

    // Events' enable
    private boolean new_event = true;
    private boolean no_event = false;
    private boolean rush_hour = false;
    private boolean first_enemy = false;
    private boolean party_time = false;
    private boolean freeze = false;
    private boolean bubble_bonus = false;
    private boolean life_bonus = false;

    // Events' number
    private static final int NO_EVENT = 0;
    private static final int PARTY_TIME = 1;
    private static final int FIRST_ENEMY = 2;
    private static final int BUBBLE_BONUS = 3;
    private static final int LIFE_BONUS = 4;
    private static final int RUSH_HOUR = 5;
    private static final int FREEZE = 6;

    // Events' data
    private static final int BONUS_BALL_MAX_NUM = 5;
    private static final int BONUS_BALL_MIN_NUM = 3;
    private static int BONUS_BALL_NUM;
    private static final int RUSH_HOUR_MAX_NUM = 30;
    private static final int RUSH_HOUR_MIN_NUM = 15;
    private static int RUSH_HOUR_NUM;
    private boolean life_detached = false;
    private boolean allBonusDetached = false;
    private static float OLD_RED;

    // Pause utils
    private boolean pause = false;
    private Point directions;
    private float old_x_speed;
    private float old_y_speed;
    private int old_game_speed;
    private long tap;


    @Override
    protected void loadGraphics() {
        super.loadGraphics();

        /** Life texture loading */
        Drawable starDraw = getResources().getDrawable(R.drawable.life);
        lifeTexture = new BitmapTextureAtlas(getTextureManager(), starDraw.getIntrinsicWidth(), starDraw.getIntrinsicHeight());
        lifeTextureRegion = createFromResource(lifeTexture, this, R.drawable.life, 0, 0);
        lifeTexture.load();

        /** Bonus ball loading */
        Drawable bonusBallDraw = getResources().getDrawable(R.drawable.ball_petrol);
        bonusBallTexture = new BitmapTextureAtlas(getTextureManager(), bonusBallDraw.getIntrinsicWidth(), bonusBallDraw.getIntrinsicHeight());
        bonusBallTextureRegion = createFromResource(bonusBallTexture, this, R.drawable.ball_petrol, 0, 0);
        bonusBallTexture.load();

    }

    @Override
    protected Scene onCreateScene() {
        super.onCreateScene();

        /** Adding the scoring text to the scene */
        txtScore = new Text(10, 10, font, "", 20, getVertexBufferObjectManager());
        scene.attachChild(txtScore);

        /** Adding the level text to the scene */
        txtLvl = new Text(10, txtScore.getY() + txtScore.getHeight(), font, "", 20, getVertexBufferObjectManager());
        scene.attachChild(txtLvl);

        /** Adding the level text to the scene */
        txtEvnt = new Text(10, txtLvl.getY() + txtLvl.getHeight(), font, "", 20, getVertexBufferObjectManager());
        scene.attachChild(txtEvnt);

        /** Adding the life sprites to the scene */
        for ( int i = 1 ; i <= MAX_LIFE ; i++ ){
            Sprite lifeSprite = new Sprite(0, 0, lifeTextureRegion,getVertexBufferObjectManager());
            lifeSprite.setX(CAMERA_WIDTH - i*lifeSprite.getWidth());
            lifeSprites.add(lifeSprite);
            scene.attachChild(lifeSprites.get(i-1));
        }

        /** The score text is updated to the current value */
        txtScore.setText("Score: " + score);

        /** Setting up the physics of the game */
        settingPhysics();

        return scene;
    }

    @Override
    public void settingPhysics() {
        /** A physics handler is linked to the ballSprite */
        handler = new PhysicsHandler(ballSprite);
        ballSprite.registerUpdateHandler(handler);

        /** The ball has the initial speed
         * - vx = BALL_SPEED
         * - vy = - BALL_SPEED
         */
        handler.setVelocity(BALL_SPEED, -BALL_SPEED);
        GAME_VELOCITY = 2;

        /** The Update Handler is linked to the scene. It evalutates the condition of the scene every frame */
        scene.registerUpdateHandler(new IUpdateHandler() {
            @Override
            public void onUpdate(float pSecondsElapsed) {
                /** Border variables */
                int rL = CAMERA_WIDTH - (int) ballSprite.getWidth() / 2;
                int bL = CAMERA_HEIGHT - (int) ballSprite.getHeight() / 2;

                /** Edge's condition
                 *  The direction of the ball changes depending on the affected side
                 */
                if(!game_over) {
                    if ((ballSprite.getX() > rL - (int) ballSprite.getWidth() / 2) && previous_event != RIGHT) {
                        handler.setVelocityX(-handler.getVelocityX());
                        touch.play();
                        previous_event = RIGHT;
                        Log.d("", "Right. V(X,Y): " + handler.getVelocityX() + "," + handler.getVelocityY());
                    }
                    if (ballSprite.getX() < 0 && previous_event != LEFT) {
                        handler.setVelocityX(-handler.getVelocityX());
                        touch.play();
                        previous_event = LEFT;
                        Log.d("", "Left. V(X,Y): " + handler.getVelocityX() + "," + handler.getVelocityY());
                    }
                    if (ballSprite.getY() < 0 && previous_event != TOP) {
                        handler.setVelocityY(-handler.getVelocityY());
                        touch.play();
                        previous_event = TOP;
                        Log.d("", "Top. V(X,Y): " + handler.getVelocityX() + "," + handler.getVelocityY());
                    }
                    if ((ballSprite.getY() > bL - (int) ballSprite.getHeight() / 2) && previous_event != BOTTOM) {
                        /** If the previous_event is "SIDE" it will reduce the undeserved points */
                        if (previous_event == SIDE)
                            remScore();
                        previous_event = BOTTOM;
                        restartOnBallLost();
                        Log.d("", "Bottom. V(X,Y): " + handler.getVelocityX() + "," + handler.getVelocityY());
                    }


                    /** When the barSprite and the ballSprite collides */
                    if (ballSprite.collidesWith(barSprite)) {
                        /** Condition variable who understand if the ball hit the bar side or front
                         * - ya: is the relative position of the ball according to the CAMERA_HEIGHT
                         * - yb: is the relative position of the ball according to the CAMERA_HEIGHT
                         */
                        float ya = ballSprite.getY() - ballSprite.getHeight() / 2;
                        float yb = barSprite.getY() - barSprite.getHeight() / 2;

                        /** The ball hit the bar's top surface */
                        if (ya <= yb && previous_event != OVER && previous_event != SIDE) {
                            Log.d("", "Top event");
                            handler.setVelocityY(-handler.getVelocityY());
                            previous_event = OVER;
                            addScore();
                        }
                        /** The ball hit the bar's side surface */
                        else if (previous_event != SIDE && previous_event != OVER) {
                            Log.d("", "Side event");
                            handler.setVelocityX(-handler.getVelocityX());
                            previous_event = SIDE;
                            addScore();
                        }
                        touch.play();
                    }

                    /** The score text is updated to the current value */
                    txtScore.setText("Score: " + score);

                    /** Game levels section */
                    gameLevels();

                    /** Game events section */
                    gameEvents();
                }
            }

            @Override
            public void reset() {

            }
        });
    }


    @Override
    public void restartOnBallLost() {
        /**  The ballSprite is detached */
        scene.detachChild(ballSprite);

        /** The lifeSprite is detached */
        scene.detachChild(lifeSprites.get(life));

        /** Life count is decremented */
        life--;

        /** If the life count is less equal than 0, the game is over */
        if(life < 0){
            game_over = true;
            txtLvl.setText("Game Over");
        }
        /** Else replace the ball */
        else{
            /** Setting the position on centre of screen */
            ballSprite.setPosition((CAMERA_WIDTH - ballSprite.getWidth())/2, (CAMERA_HEIGHT - ballSprite.getHeight())/2);

            /** Set the direction upward */
            handler.setVelocityY(-handler.getVelocityY());

            /** The ballSprite is attached */
            scene.attachChild(ballSprite);
        }
    }

    @Override
    public void addScore() {
        /** This procedure increase the score according to the current score. */
        if(score >= 0 && score < BARRIER_ONE) {
            score += 2;
            gain = 2;
        }

        if(score >= BARRIER_ONE && score < BARRIER_TWO) {
            score += 5;
            gain = 5;
        }

        if(score >= BARRIER_TWO && score < BARRIER_THREE) {
            score += 15;
            gain = 15;
        }

        if(score >= BARRIER_THREE && score < BARRIER_FOUR) {
            score += 30;
            gain = 30;
        }

        if(score >= BARRIER_FOUR && score < BARRIER_FIVE) {
            score += 60;
            gain = 60;
        }

        if(score >= BARRIER_FIVE && score < BARRIER_SIX) {
            score += 90;
            gain = 90;
        }

        if(score >= BARRIER_SEVEN) {
            score += 150;
            gain = 150;
        }

        if(game_event == PARTY_TIME)
            score += gain;

        if(game_event == RUSH_HOUR)
            score += gain*4;

        if(game_event == FIRST_ENEMY)
            score += gain*3;
    }

    @Override
    public void remScore() {
        // do nothing
    }

    @Override
    public void actionDownEvent() {
        if(!pause) {
            directions = getDirections();
            old_x_speed = handler.getVelocityX();
            old_y_speed = handler.getVelocityY();
            old_game_speed = GAME_VELOCITY;
            tap = System.currentTimeMillis();
            handler.setVelocity(0);
            GAME_VELOCITY = 0;
            pause = true;
        }

        if(pause && (System.currentTimeMillis() - tap > 500)){
            handler.setVelocity(old_x_speed,old_y_speed);
            GAME_VELOCITY = old_game_speed;
            pause = false;
        }
    }


    private void gameLevels(){
        /** This procedure understand what modifier needs according to the score */
        if(score < BARRIER_ONE && level_one){
            level = LEVEL_ONE;
            txtLvl.setText("Level one");
        }

        if(score >= BARRIER_ONE && score < BARRIER_TWO && !level_two) {
            level = LEVEL_TWO;
            GAME_VELOCITY *= 2;
            level_two = true;
            txtLvl.setText("Level two");
        }

        if(score >= BARRIER_TWO && score < BARRIER_THREE && !level_three) {
            level = LEVEL_THREE;
            handler.setVelocity(handler.getVelocityX() * 2, handler.getVelocityY() * 2);
            level_three = true;
            txtLvl.setText("Level three");
        }

        if(score >= BARRIER_THREE && score < BARRIER_FOUR && !level_four) {
            level = LEVEL_FOUR;
            barSprite.setWidth(0.2f * CAMERA_WIDTH);
            level_four = true;
            txtLvl.setText("Level four");
        }

        if(score >= BARRIER_FOUR && score < BARRIER_FIVE && !level_five) {
            level = LEVEL_FIVE;
            handler.setVelocity(handler.getVelocityX() * 1.5f, handler.getVelocityY() * 1.5f);
            barSprite.setWidth(0.3f * CAMERA_WIDTH);
            level_five = true;
            txtLvl.setText("Level five");
        }

        if(score >= BARRIER_FIVE && score < BARRIER_SIX && !level_six) {
            level = LEVEL_SIX;
            barSprite.setWidth(0.2f * CAMERA_WIDTH);
            level_six = true;
            txtLvl.setText("Level six");
        }

        if(score >= BARRIER_SEVEN && !level_seven) {
            level = LEVEL_SEVEN;
            handler.setVelocity(handler.getVelocityX() * 1.5f, handler.getVelocityY() * 1.5f);
            level_seven = true;
            txtLvl.setText("Level seven");
        }
    }

    private void randomEventsGenerator(){
        if(new_event) {
            Timer timer = new Timer();
            Random random = new Random();
            timer.schedule(new SetGameEvents(), 5000 + random.nextInt(5000));
            new_event = false;
        }
    }

    private void gameEvents(){

        /** Setting up random events generator */
        randomEventsGenerator();

        /** Handling game events collisions */
        gameEventsCollisionLogic();

        /** Handling events logic */
        switch(game_event){
            case NO_EVENT:
                if(!no_event) {
                    txtEvnt.setText("");
                    no_event = true;
                    noEventLogic();
                }
                break;

            case RUSH_HOUR:
                if(!rush_hour){
                    txtEvnt.setText("Rush Hour");
                    rush_hour = true;
                    rushHourLogic();
                }
                break;

            case PARTY_TIME:
                if(!party_time){
                    txtEvnt.setText("Party Time");
                    party_time = true;
                    partyTimeLogic();
                }
                break;

            case FIRST_ENEMY:
                if(!first_enemy){
                    txtEvnt.setText("First enemy");
                    first_enemy = true;
                    firstEnemyLogic();
                }
                break;

            case BUBBLE_BONUS:
                if(!bubble_bonus){
                    txtEvnt.setText("Bubble bonus");
                    bubble_bonus = true;
                    bubbleBonusLogic();
                }
                break;

            case FREEZE:
                if(!freeze){
                    txtEvnt.setText("Freeze");
                    freeze = true;
                    freezeLogic();
                }
                break;

            case LIFE_BONUS:
                if(!life_bonus){
                    txtEvnt.setText("Life Bonus");
                    life_bonus = true;
                    lifeBonusLogic();
                }
                break;
        }

    }

    private void noEventLogic(){
        // do nothing
    }

    private void rushHourLogic(){
        Random random = new Random();

        RUSH_HOUR_NUM = RUSH_HOUR_MIN_NUM + random.nextInt(RUSH_HOUR_MAX_NUM - RUSH_HOUR_MIN_NUM);

        for(int i = 0 ; i < RUSH_HOUR_NUM ; i++){
            Sprite rush = new Sprite(0, 0, ballTextureRegion, getVertexBufferObjectManager());
            rush.setPosition(random.nextInt(CAMERA_WIDTH), random.nextInt(CAMERA_HEIGHT) - ballSprite.getHeight()*2);
            rush.setWidth(CAMERA_WIDTH * 0.1f);
            rush.setHeight(CAMERA_WIDTH * 0.1f);
            rushHour.add(rush);

            PhysicsHandler physicsHandler = new PhysicsHandler(rushHour.get(i));
            physicsHandler.setVelocity(BALL_SPEED * (random.nextFloat() - random.nextFloat()), BALL_SPEED * (random.nextFloat() - random.nextFloat()) );
            rushHourHandlers.add(physicsHandler);

            rushHour.get(i).registerUpdateHandler(rushHourHandlers.get(i));

            scene.attachChild(rushHour.get(i));
        }
    }

    private void clearRushHour(){
        do {
            scene.detachChild(rushHour.get(0));
            rushHour.remove(0);
            rushHourHandlers.remove(0);
        } while ( rushHour.size() > 0 );

        rush_hour = false;
    }

    private void partyTimeLogic(){
        barSprite.setRed(0.99f);
        OLD_RED = barSprite.getRed();
    }

    private void clearPartyTime(){
        barSprite.setRed(OLD_RED);
        party_time = false;
    }

    private void firstEnemyLogic(){
        firstEnemy = new Sprite(0, CAMERA_HEIGHT/3, barTextureRegion, getVertexBufferObjectManager());
        firstEnemy.setWidth(CAMERA_WIDTH);
        scene.attachChild(firstEnemy);
    }

    private void clearFirstEnemyLogic(){
        first_enemy = false;
        scene.detachChild(firstEnemy);
    }

    private void bubbleBonusLogic(){
        Random random = new Random();

        BONUS_BALL_NUM = BONUS_BALL_MIN_NUM + random.nextInt(BONUS_BALL_MAX_NUM - BONUS_BALL_MIN_NUM);

        /** Adding the life sprites to the scene */
        for ( int i = 0 ; i < BONUS_BALL_NUM ; i++ ){
            Sprite bonusSprite = new Sprite(0, 0, bonusBallTextureRegion, getVertexBufferObjectManager());
            bonusSprite.setPosition(random.nextInt(CAMERA_WIDTH), random.nextInt(CAMERA_HEIGHT) - ballSprite.getHeight()*2);
            bonusSprite.setWidth(CAMERA_WIDTH * 0.1f);
            bonusSprite.setHeight(CAMERA_WIDTH * 0.1f);
            bonusBalls.add(bonusSprite);
            scene.attachChild(bonusBalls.get(i));
        }
    }

    private void clearBubbleBonus(){
        if(!allBonusDetached) {
            do {
                scene.detachChild(bonusBalls.get(0));
                bonusBalls.remove(0);
            } while (bonusBalls.size() > 0);
        }
        bubble_bonus = false;
}

    private void lifeBonusLogic(){
        if(life < MAX_LIFE - 1) {
            Random random = new Random();
            lifeBonus = new Sprite(0, 0, lifeTextureRegion, getVertexBufferObjectManager());
            lifeBonus.setWidth(CAMERA_WIDTH * 0.2f);
            lifeBonus.setHeight(CAMERA_WIDTH * 0.2f);
            lifeBonus.setPosition(random.nextInt(CAMERA_WIDTH - (int)lifeBonus.getWidth()), random.nextInt(CAMERA_HEIGHT - 2*(int)ballSprite.getHeight()));
            scene.attachChild(lifeBonus);
        }
    }

    private void clearLifeBonus(){
        if(!life_detached)
            scene.detachChild(lifeBonus);
        life_bonus = false;
        life_detached = false;
    }

    private void gameEventsCollisionLogic(){
        switch (game_event){
            case FIRST_ENEMY:
                if(ballSprite.collidesWith(firstEnemy) && first_enemy && ballSprite.getY() < CAMERA_HEIGHT/2){
                    handler.setVelocityY(-handler.getVelocityY());
                    touch.play();
                }
                break;

            case BUBBLE_BONUS:
                for ( int i = 0 ; i < bonusBalls.size() ; i++ ){
                    if(ballSprite.collidesWith(bonusBalls.get(i))){
                        scene.detachChild(bonusBalls.get(i));
                        bonusBalls.remove(i);
                        score += 20 * (level+1);
                        if(bonusBalls.size()==0)
                            allBonusDetached = true;
                    }
                }
                break;

            case RUSH_HOUR:
                rushHourCollisions();
                break;

            case LIFE_BONUS:
                if(ballSprite.collidesWith(lifeBonus) && life < MAX_LIFE - 1){
                    life++;
                    scene.attachChild(lifeSprites.get(life));
                    scene.detachChild(lifeBonus);
                    life_detached = true;
                }
                break;
        }

    }

    private void rushHourCollisions(){
        int rL = CAMERA_WIDTH - (int) ballSprite.getWidth() / 2;
        int bL = CAMERA_HEIGHT - (int) ballSprite.getHeight() / 2;

        for(int i = 0 ; i < rushHour.size() ; i++){
            if ((rushHour.get(i).getX() > rL - (int) ballSprite.getWidth() / 2)) {
                rushHourHandlers.get(i).setVelocityX(-rushHourHandlers.get(i).getVelocityX());
            }
            if (rushHour.get(i).getX() < 0) {
                rushHourHandlers.get(i).setVelocityX(-rushHourHandlers.get(i).getVelocityX());
            }
            if (rushHour.get(i).getY() < 0) {
                rushHourHandlers.get(i).setVelocityY(-rushHourHandlers.get(i).getVelocityY());
            }
            if ((rushHour.get(i).getY() > bL - (int) ballSprite.getHeight() / 2)) {
                rushHourHandlers.get(i).setVelocityY(-rushHourHandlers.get(i).getVelocityY());
            }
        }
    }

    private void freezeLogic(){
        handler.setVelocity(handler.getVelocityX()/2, handler.getVelocityY()/2);
    }

    private void clearFreeze(){
        handler.setVelocity(handler.getVelocityX()*2, handler.getVelocityY()*2);
        freeze = false;
    }



    private void clearEvents() {
        switch (game_event) {
            case NO_EVENT:
                no_event = false;
                break;

            case PARTY_TIME:
                clearPartyTime();
                break;

            case FIRST_ENEMY:
                clearFirstEnemyLogic();
                break;

            case BUBBLE_BONUS:
                clearBubbleBonus();
                break;

            case RUSH_HOUR:
                clearRushHour();
                break;

            case FREEZE:
                clearFreeze();
                break;

            case LIFE_BONUS:
                clearLifeBonus();
                break;
        }
    }

        class SetGameEvents extends TimerTask {

            @Override
            public void run() {
                clearEvents();

                if (level > LEVEL_ONE && !pause) {
                    Random random = new Random();
                    game_event = random.nextInt(level * 1000) % (level+1);
                    Log.d("","Evento " + game_event);
                }

                new_event = true;
            }
        }


}
