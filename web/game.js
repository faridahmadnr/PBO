const WIDTH = 800;
const HEIGHT = 600;
const FPS = 60;
const FRAME_MS = 1000 / FPS;

const STATE_MENU = 0;
const STATE_PLAYING = 1;
const STATE_GAMEOVER = 2;

const canvas = document.getElementById("gameCanvas");
const ctx = canvas.getContext("2d");
const muteBtn = document.getElementById("muteBtn");

const assets = {
  p1Img: new Image(),
  p2Img: new Image(),
  bgMusic: new Audio("assets/background_music.wav"),
  collisionSfx: new Audio("assets/collision.wav"),
  gameOverSfx: new Audio("assets/gameover.wav"),
};

// Matches Game.java: player1 uses player2.png, player2 uses player1.png
assets.p1Img.src = "assets/player2.png";
assets.p2Img.src = "assets/player1.png";
assets.bgMusic.loop = true;
assets.bgMusic.volume = 0.45;
assets.collisionSfx.volume = 0.7;
assets.gameOverSfx.volume = 0.7;

let isMuted = false;

muteBtn.addEventListener("click", () => {
  isMuted = !isMuted;
  assets.bgMusic.muted = isMuted;
  assets.collisionSfx.muted = isMuted;
  assets.gameOverSfx.muted = isMuted;
  muteBtn.textContent = isMuted ? "🔇 Sound: OFF" : "🔊 Sound: ON";
  muteBtn.setAttribute("aria-pressed", String(isMuted));
  canvas.focus();
});

function playSound(audio) {
  if (isMuted) return;
  try {
    audio.currentTime = 0;
    const promise = audio.play();
    if (promise && typeof promise.catch === "function") {
      promise.catch(() => {});
    }
  } catch {
    // Ignore autoplay errors
  }
}

class Player {
  constructor(x, y, color, lives, image) {
    this.x = x;
    this.y = y;
    this.size = 40;
    this.speed = 5;
    this.color = color;
    this.lives = lives;
    this.image = image;
    this.movingUp = false;
    this.movingDown = false;
    this.movingLeft = false;
    this.movingRight = false;
  }

  update(screenWidth, screenHeight) {
    if (this.movingUp) this.y = Math.max(this.y - this.speed, 0);
    if (this.movingDown) this.y = Math.min(this.y + this.speed, screenHeight - this.size);
    if (this.movingLeft) this.x = Math.max(this.x - this.speed, 0);
    if (this.movingRight) this.x = Math.min(this.x + this.speed, screenWidth - this.size);
  }

  draw(context) {
    if (this.image && this.image.complete && this.image.naturalWidth > 0) {
      context.drawImage(this.image, this.x, this.y, this.size, this.size);
    } else {
      context.fillStyle = this.color;
      context.fillRect(this.x, this.y, this.size, this.size);
    }

    context.fillStyle = this.color;
    for (let i = 0; i < this.lives; i++) {
      context.fillRect(this.x + i * 10, this.y - 15, 8, 8);
    }
  }

  collidesWithPlayer(other) {
    return (
      this.x < other.x + other.size &&
      this.x + this.size > other.x &&
      this.y < other.y + other.size &&
      this.y + this.size > other.y
    );
  }
}

class Rocket {
  constructor(x, y, target) {
    this.x = x;
    this.y = y;
    this.size = 20;
    this.speed = 3;
    this.lifetime = 3000;
    this.target = target;
    this.spawnTime = performance.now();
  }

  update() {
    const targetX = this.target.x + this.target.size / 2;
    const targetY = this.target.y + this.target.size / 2;
    const dx = targetX - this.x;
    const dy = targetY - this.y;
    const distance = Math.hypot(dx, dy);

    if (distance > 0) {
      this.x += (dx / distance) * this.speed;
      this.y += (dy / distance) * this.speed;
    }
  }

  draw(context) {
    const angle = Math.atan2(
      this.target.y + this.target.size / 2 - this.y,
      this.target.x + this.target.size / 2 - this.x
    );

    context.save();
    context.translate(this.x, this.y);
    context.rotate(angle);

    // Exhaust flame
    context.fillStyle = "#f97316";
    context.beginPath();
    context.moveTo(-10, -4);
    context.lineTo(-18 - Math.random() * 4, 0);
    context.lineTo(-10, 4);
    context.closePath();
    context.fill();

    // Rocket body
    context.fillStyle = "#e2e8f0";
    context.beginPath();
    context.moveTo(10, 0);
    context.lineTo(-8, -6);
    context.lineTo(-5, 0);
    context.lineTo(-8, 6);
    context.closePath();
    context.fill();

    // Warhead tip
    context.fillStyle = "#ef4444";
    context.beginPath();
    context.moveTo(10, 0);
    context.lineTo(3, -3.5);
    context.lineTo(3, 3.5);
    context.closePath();
    context.fill();

    context.restore();
  }

  isExpired(now) {
    return now - this.spawnTime > this.lifetime;
  }

  collidesWith(player) {
    const rx = this.x - this.size / 2;
    const ry = this.y - this.size / 2;
    return (
      rx < player.x + player.size &&
      rx + this.size > player.x &&
      ry < player.y + player.size &&
      ry + this.size > player.y
    );
  }
}

class ExplosionAnimation {
  constructor(x, y) {
    this.x = x;
    this.y = y;
    this.totalFrames = 5;
    this.frameDuration = 100;
    this.createdAt = performance.now();
    this.active = true;
  }

  update(now) {
    const frame = Math.floor((now - this.createdAt) / this.frameDuration);
    if (frame >= this.totalFrames) {
      this.active = false;
    }
  }

  draw(context, now) {
    if (!this.active) return;
    const frame = Math.min(
      this.totalFrames - 1,
      Math.floor((now - this.createdAt) / this.frameDuration)
    );
    const progress = (frame + 1) / this.totalFrames;
    const radius = 16 + progress * 34;

    context.save();
    context.globalAlpha = 1 - progress * 0.65;

    const grad = context.createRadialGradient(this.x, this.y, 4, this.x, this.y, radius);
    grad.addColorStop(0, "#ffffff");
    grad.addColorStop(0.35, "#facc15");
    grad.addColorStop(0.7, "#f97316");
    grad.addColorStop(1, "rgba(239, 68, 68, 0)");

    context.fillStyle = grad;
    context.beginPath();
    context.arc(this.x, this.y, radius, 0, Math.PI * 2);
    context.fill();
    context.restore();
  }
}

let gameState = STATE_MENU;
let player1 = new Player(100, 300, "#3b82f6", 3, assets.p1Img);
let player2 = new Player(700, 300, "#ef4444", 3, assets.p2Img);
let rockets = [];
let explosions = [];
let gameOver = false;
let gameStartTime = 0;
let currentSurvivalTime = 0;
let finalSurvivalTime = 0;
let difficultyLevel = 1;
let maxRocketsPerPlayer = 2;

function startGame() {
  player1 = new Player(100, 300, "#3b82f6", 3, assets.p1Img);
  player2 = new Player(700, 300, "#ef4444", 3, assets.p2Img);
  rockets = [];
  explosions = [];
  gameOver = false;
  difficultyLevel = 1;
  maxRocketsPerPlayer = 2;
  gameStartTime = performance.now();
  currentSurvivalTime = 0;
  gameState = STATE_PLAYING;
  playSound(assets.bgMusic);
}

function setGameOverState() {
  finalSurvivalTime = currentSurvivalTime;
  gameOver = true;
  gameState = STATE_GAMEOVER;
  assets.bgMusic.pause();
  assets.bgMusic.currentTime = 0;
  playSound(assets.gameOverSfx);
}

function updateSurvivalTime(now) {
  if (gameState === STATE_PLAYING && !gameOver) {
    currentSurvivalTime = Math.floor((now - gameStartTime) / 1000);
    const newDifficultyLevel = Math.floor(currentSurvivalTime / 5) + 1;
    if (newDifficultyLevel > difficultyLevel) {
      difficultyLevel = newDifficultyLevel;
      maxRocketsPerPlayer = 2 + (difficultyLevel - 1);
    }
  }
}

function checkAllPlayersDead() {
  if (player1.lives <= 0 && player2.lives <= 0 && !gameOver) {
    setGameOverState();
  }
}

function spawnRocket(target) {
  if (target.lives <= 0) return;
  const side = Math.floor(Math.random() * 4);
  let x = 0;
  let y = 0;

  if (side === 0) {
    x = Math.random() * WIDTH;
    y = 0;
  } else if (side === 1) {
    x = WIDTH;
    y = Math.random() * HEIGHT;
  } else if (side === 2) {
    x = Math.random() * WIDTH;
    y = HEIGHT;
  } else {
    x = 0;
    y = Math.random() * HEIGHT;
  }

  rockets.push(new Rocket(x, y, target));
}

function spawnRockets() {
  if (gameState !== STATE_PLAYING) return;

  if (player1.lives > 0) {
    let p1Count = rockets.filter((r) => r.target === player1).length;
    while (p1Count < maxRocketsPerPlayer) {
      spawnRocket(player1);
      p1Count++;
    }
  }

  if (player2.lives > 0) {
    let p2Count = rockets.filter((r) => r.target === player2).length;
    while (p2Count < maxRocketsPerPlayer) {
      spawnRocket(player2);
      p2Count++;
    }
  }
}

function update(now) {
  if (gameState !== STATE_PLAYING) return;

  updateSurvivalTime(now);
  checkAllPlayersDead();

  if (player1.lives > 0) player1.update(WIDTH, HEIGHT);
  if (player2.lives > 0) player2.update(WIDTH, HEIGHT);

  if (player1.lives > 0 && player2.lives > 0 && player1.collidesWithPlayer(player2)) {
    explosions.push(new ExplosionAnimation(player1.x, player1.y));
    explosions.push(new ExplosionAnimation(player2.x, player2.y));
    player1.lives = 0;
    player2.lives = 0;
    playSound(assets.collisionSfx);
    setGameOverState();
    return;
  }

  const remainingRockets = [];
  for (const rocket of rockets) {
    rocket.update();

    if (
      (rocket.target === player1 && player1.lives <= 0) ||
      (rocket.target === player2 && player2.lives <= 0)
    ) {
      continue;
    }

    if (rocket.isExpired(now)) {
      continue;
    }

    if (player1.lives > 0 && rocket.collidesWith(player1)) {
      player1.lives--;
      playSound(assets.collisionSfx);
      if (player1.lives <= 0) {
        explosions.push(new ExplosionAnimation(player1.x, player1.y));
        if (player2.lives <= 0) setGameOverState();
      }
      continue;
    }

    if (player2.lives > 0 && rocket.collidesWith(player2)) {
      player2.lives--;
      playSound(assets.collisionSfx);
      if (player2.lives <= 0) {
        explosions.push(new ExplosionAnimation(player2.x, player2.y));
        if (player1.lives <= 0) setGameOverState();
      }
      continue;
    }

    remainingRockets.push(rocket);
  }

  rockets = remainingRockets;

  for (const explosion of explosions) {
    explosion.update(now);
  }
  explosions = explosions.filter((e) => e.active);

  checkAllPlayersDead();
}

function drawMenuScreen() {
  ctx.fillStyle = "#000000";
  ctx.fillRect(0, 0, WIDTH, HEIGHT);

  ctx.textAlign = "center";

  ctx.fillStyle = "#facc15";
  ctx.font = "bold 50px Arial, sans-serif";
  ctx.fillText("ROCKET DODGE", WIDTH / 2, HEIGHT / 3);

  ctx.fillStyle = "#ffffff";
  ctx.font = "25px Arial, sans-serif";
  ctx.fillText("Press SPACE or Click to Start", WIDTH / 2, HEIGHT / 2);

  ctx.fillStyle = "#22d3ee";
  ctx.font = "20px Arial, sans-serif";
  ctx.fillText("SURVIVAL MODE: Dodge rockets as long as possible!", WIDTH / 2, HEIGHT / 2 + 40);

  ctx.fillStyle = "#fb923c";
  ctx.font = "italic 18px Arial, sans-serif";
  ctx.fillText("Difficulty increases every 5 seconds!", WIDTH / 2, HEIGHT / 2 + 70);

  ctx.textAlign = "left";
  ctx.fillStyle = "#ffffff";
  ctx.font = "20px Arial, sans-serif";
  ctx.fillText("Player 1: WASD to move", 50, HEIGHT - 120);
  ctx.fillText("Player 2: Arrow keys to move", 50, HEIGHT - 90);
  ctx.fillText("Avoid rockets and don't crash into each other!", 50, HEIGHT - 60);
}

function drawGameScreen(now) {
  ctx.fillStyle = "#000000";
  ctx.fillRect(0, 0, WIDTH, HEIGHT);

  if (player1.lives > 0) player1.draw(ctx);
  if (player2.lives > 0) player2.draw(ctx);

  for (const rocket of rockets) {
    rocket.draw(ctx);
  }

  for (const explosion of explosions) {
    explosion.draw(ctx, now);
  }

  ctx.textAlign = "left";
  ctx.font = "14px Arial, sans-serif";
  ctx.fillStyle = "#ffffff";
  ctx.fillText(`Player 1 Lives: ${player1.lives}`, 10, 20);
  ctx.fillText(`Player 2 Lives: ${player2.lives}`, WIDTH - 125, 20);

  ctx.fillStyle = "#facc15";
  ctx.fillText(`Time: ${currentSurvivalTime} seconds`, WIDTH / 2 - 60, 20);

  ctx.fillStyle = "#fb923c";
  ctx.fillText(`Difficulty: ${difficultyLevel}`, WIDTH / 2 - 60, 40);

  if (gameState === STATE_GAMEOVER) {
    ctx.textAlign = "center";
    ctx.fillStyle = "#ef4444";
    ctx.font = "bold 34px Arial, sans-serif";
    ctx.fillText("GAME OVER", WIDTH / 2, HEIGHT / 2 - 20);

    ctx.fillStyle = "#facc15";
    ctx.font = "bold 25px Arial, sans-serif";
    ctx.fillText(`You survived for ${finalSurvivalTime} seconds!`, WIDTH / 2, HEIGHT / 2 + 20);

    ctx.fillStyle = "#ffffff";
    ctx.font = "20px Arial, sans-serif";
    ctx.fillText("Press ENTER or Click to return to menu", WIDTH / 2, HEIGHT / 2 + 60);
  }
}

function render(now) {
  if (gameState === STATE_MENU) {
    drawMenuScreen();
  } else {
    drawGameScreen(now);
  }
}

function setMovementKey(key, pressed) {
  if (key === "w" || key === "W") player1.movingUp = pressed;
  if (key === "s" || key === "S") player1.movingDown = pressed;
  if (key === "a" || key === "A") player1.movingLeft = pressed;
  if (key === "d" || key === "D") player1.movingRight = pressed;

  if (key === "ArrowUp") player2.movingUp = pressed;
  if (key === "ArrowDown") player2.movingDown = pressed;
  if (key === "ArrowLeft") player2.movingLeft = pressed;
  if (key === "ArrowRight") player2.movingRight = pressed;
}

window.addEventListener("keydown", (e) => {
  if ([" ", "ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight"].includes(e.key)) {
    e.preventDefault();
  }

  if (gameState === STATE_MENU && e.code === "Space") {
    startGame();
    return;
  }

  if (gameState === STATE_PLAYING) {
    setMovementKey(e.key, true);
    if (e.key === "x" || e.key === "X") {
      explosions.push(new ExplosionAnimation(player1.x, player1.y));
    }
  }

  if (gameState === STATE_GAMEOVER && e.key === "Enter") {
    gameState = STATE_MENU;
  }
});

window.addEventListener("keyup", (e) => {
  if (gameState === STATE_PLAYING) {
    setMovementKey(e.key, false);
  }
});

canvas.addEventListener("click", () => {
  canvas.focus();
  if (gameState === STATE_MENU) {
    startGame();
  } else if (gameState === STATE_GAMEOVER) {
    gameState = STATE_MENU;
  }
});

let lastTime = performance.now();
let delta = 0;
let spawnTimer = performance.now();

function gameLoop(now) {
  delta += (now - lastTime) / FRAME_MS;
  lastTime = now;

  // Cap delta to avoid spiral of death after tab switch
  if (delta > 5) delta = 5;

  while (delta >= 1) {
    update(now);
    delta--;
  }

  if (now - spawnTimer > 1000) {
    spawnTimer += 1000;
    spawnRockets();
  }

  render(now);
  requestAnimationFrame(gameLoop);
}

requestAnimationFrame(gameLoop);
