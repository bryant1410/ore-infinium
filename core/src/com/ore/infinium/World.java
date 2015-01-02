package com.ore.infinium;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.PooledEngine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.ore.infinium.components.*;

/**
 * ***************************************************************************
 * Copyright (C) 2014 by Shaun Reich <sreich@kde.org>                        *
 * *
 * This program is free software; you can redistribute it and/or             *
 * modify it under the terms of the GNU General Public License as            *
 * published by the Free Software Foundation; either version 2 of            *
 * the License, or (at your option) any later version.                       *
 * *
 * This program is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 * GNU General Public License for more details.                              *
 * *
 * You should have received a copy of the GNU General Public License         *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.     *
 * ***************************************************************************
 */
public class World implements Disposable {
    public static final float PIXELS_PER_METER = 50.0f;
    public static final float BLOCK_SIZE = (16.0f / PIXELS_PER_METER);
    public static final float BLOCK_SIZE_PIXELS = 16.0f;
    public static final int WORLD_COLUMNCOUNT = 2400;
    public static final int WORLD_ROWCOUNT = 8400;
    public static final int WORLD_SEA_LEVEL = 50;

    public Block[] blocks;
    public PooledEngine engine;
    public AssetManager assetManager;
    public Array<Entity> m_players = new Array<Entity>();

    private SpriteBatch m_batch;
    private Texture m_texture;
    private Sprite m_mainPlayer;
    private Sprite m_sprite2;

    private TileRenderer m_tileRenderer;
    private SpriteRenderer m_spriteRenderer;

    private OrthographicCamera m_camera;
    private OreServer m_server;
    private OreClient m_client;

    public World(OreClient client, OreServer server) {
        m_client = client;
        m_server = server;

        if (isClient()) {
            float w = Gdx.graphics.getWidth();
            float h = Gdx.graphics.getHeight();
            m_batch = new SpriteBatch();
            m_spriteRenderer = new SpriteRenderer();
        }

        blocks = new Block[WORLD_ROWCOUNT * WORLD_COLUMNCOUNT];
        for (int x = 0; x < WORLD_COLUMNCOUNT; ++x) {
            for (int y = 0; y < WORLD_ROWCOUNT; ++y) {
                blocks[x * WORLD_ROWCOUNT + y] = new Block();
            }
        }

//        assetManager = new AssetManager();
//        TextureAtlas atlas = assetManager.get("data/", TextureAtlas.class);
//        assetManager.finishLoading();

        engine = new PooledEngine(2000, 2000, 2000, 2000);


        m_mainPlayer = new Sprite();
        m_mainPlayer.setPosition(50, 50);

        m_sprite2 = new Sprite();
        m_sprite2.setPosition(90, 90);

        m_camera = new OrthographicCamera(1600 / World.PIXELS_PER_METER, 900 / World.PIXELS_PER_METER);//30, 30 * (h / w));
        m_camera.setToOrtho(true, 1600 / World.PIXELS_PER_METER, 900 / World.PIXELS_PER_METER);

//        m_camera.position.set(m_camera.viewportWidth / 2f, m_camera.viewportHeight / 2f, 0);
        m_camera.position.set(m_mainPlayer.getX(), m_mainPlayer.getY(), 0);
        m_camera.update();

        if (isClient()) {
            m_texture = new Texture(Gdx.files.internal("crap.png"));
            m_mainPlayer.setTexture(m_texture);
            m_sprite2.setTexture(m_texture);
            m_tileRenderer = new TileRenderer(m_camera, this, m_mainPlayer);
        }

        generateWorld();
    }

    public void initServer() {
    }

    private void generateWorld() {
        generateNoise();
    }

    private void generateNoise() {
//        for (int x = 0; x < WORLD_COLUMNCOUNT; ++x) {
//            for (int y = seaLevel(); y < WORLD_ROWCOUNT; ++y) {
//                Block block = blockAt(x, y);
//                block.blockType = Block.BlockType.DirtBlockType;
//            }
//        }
    }

    public boolean isServer() {
        return m_server != null;
    }

    public boolean isClient() {
        return m_client != null;
    }

    public Block blockAtPosition(Vector2 pos) {
        return blockAt((int) (pos.x / BLOCK_SIZE), (int) (pos.y / BLOCK_SIZE));
    }

    public Block blockAt(int x, int y) {
        assert x >= 0 && y >= 0 && x <= WORLD_COLUMNCOUNT && y <= WORLD_ROWCOUNT;

        return blocks[x * WORLD_ROWCOUNT + y];
    }

    public boolean isBlockSolid(int x, int y) {
        boolean solid = true;

        Block.BlockType type = blockAt(x, y).blockType;

        if (type == Block.BlockType.NullBlockType) {
            solid = false;
        }

        return solid;
    }

    public boolean canPlaceBlock(int x, int y) {
        boolean canPlace = blockAt(x, y).blockType == Block.BlockType.NullBlockType;
        //TODO: check collision with other entities...

        return canPlace;
    }

    public void dispose() {
        m_batch.dispose();
        m_texture.dispose();
    }

    public void zoom(float factor) {

        m_camera.zoom *= factor;
    }

    public void update(double elapsed) {

        engine.update((float) elapsed);
    }

    public void render(double elapsed) {
//        m_camera.zoom *= 0.9;
        //m_lightRenderer->renderToFBO();

        m_tileRenderer.render(elapsed);

        //FIXME: incorporate entities into the pre-lit gamescene FBO, then render lighting as last pass
        //m_lightRenderer->renderToBackbuffer();

        m_spriteRenderer.renderEntities(elapsed);
        m_spriteRenderer.renderCharacters(elapsed);
        m_spriteRenderer.renderDroppedEntities(elapsed);
        m_spriteRenderer.renderDroppedBlocks(elapsed);


        //FIXME: take lighting into account, needs access to fbos though.
        //   m_fluidRenderer->render();
//    m_particleRenderer->render();
//FIXME unused    m_quadTreeRenderer->render();

        updateCrosshair();
        updateItemPlacementGhost();

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            m_mainPlayer.translateX(-PlayerComponent.movementSpeed);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            m_mainPlayer.translateX(PlayerComponent.movementSpeed);
        }

        m_camera.position.set(m_mainPlayer.getX(), m_mainPlayer.getY(), 0);
        m_camera.update();

        m_batch.setProjectionMatrix(m_camera.combined);

        m_tileRenderer.render(elapsed);

        m_batch.begin();
        //m_batch.draw
        m_mainPlayer.draw(m_batch);
        m_sprite2.draw(m_batch);
        m_batch.end();
    }

    private void updateCrosshair() {
    }

    private void updateItemPlacementGhost() {

    }

    public int seaLevel() {
        return WORLD_SEA_LEVEL;
    }

    public void createBlockItem(Entity block) {
        block.add(engine.createComponent(VelocityComponent.class));

        BlockComponent blockComponent = engine.createComponent(BlockComponent.class);
        blockComponent.blockType = Block.BlockType.StoneBlockType;
        block.add(blockComponent);

        SpriteComponent blockSprite = engine.createComponent(SpriteComponent.class);
        blockSprite.texture = "pickaxeWooden1"; // HACK ?

//warning fixme size is fucked
        blockSprite.sprite.setSize(32 / World.PIXELS_PER_METER, 32 / World.PIXELS_PER_METER);
        block.add(blockSprite);

        ItemComponent itemComponent = engine.createComponent(ItemComponent.class);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;
        block.add(itemComponent);
    }

    public Entity createAirGenerator() {
        Entity air = engine.createEntity();
        ItemComponent itemComponent = engine.createComponent(ItemComponent.class);
        itemComponent.stackSize = 800;
        itemComponent.maxStackSize = 900;
        air.add(itemComponent);


        SpriteComponent airSprite = engine.createComponent(SpriteComponent.class);
        airSprite.texture = "airGenerator1";

//warning fixme size is fucked
        airSprite.sprite.setSize(BLOCK_SIZE * 4, BLOCK_SIZE * 4);
        air.add(airSprite);

        AirGeneratorComponent airComponent = engine.createComponent(AirGeneratorComponent.class);
        airComponent.airOutputRate = 100;
        air.add(airComponent);

        return air;
    }

    public void addPlayer(Entity player) {
        m_players.add(player);
    }
}
