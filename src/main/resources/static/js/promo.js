/**
 * Zentrix 10s Premium Cinematic Advertisement Player
 * Driven by HTML5 Canvas (Visuals & Camera) + CSS Animations + Web Audio API (Synth Music)
 */

class ZentrixPromoPlayer {
    constructor() {
        // Player state
        this.duration = 10.0; // 10 seconds total
        this.currentTime = 0.0;
        this.isPlaying = true;
        this.isMuted = true;
        this.currentSceneIndex = 0;
        
        // Timing boundaries for scenes (0-indexed, 7 scenes)
        this.scenes = [
            { id: 1, name: "City Intro", start: 0.0, end: 1.0, selector: "#scene-1" },
            { id: 2, name: "Learning", start: 1.0, end: 3.0, selector: "#scene-2" },
            { id: 3, name: "Voting", start: 3.0, end: 5.0, selector: "#scene-3" },
            { id: 4, name: "Gaming Arena", start: 5.0, end: 6.0, selector: "#scene-4" },
            { id: 5, name: "Concerts & booking", start: 6.0, end: 8.0, selector: "#scene-5" },
            { id: 6, name: "Rewards Chest", start: 8.0, end: 9.0, selector: "#scene-6" },
            { id: 7, name: "Future Zentrix", start: 9.0, end: 10.0, selector: "#scene-7" }
        ];

        // Particle configuration
        this.particles = [];
        this.maxParticles = 50;

        // Coin burst particles (Scene 6)
        this.coins = [];
        this.coinBurstTriggered = false;

        // Sound configuration
        this.audioCtx = null;
        this.synthLoopId = null;
        this.gainNode = null;

        // Binding DOM elements
        this.initDOMElements();
        this.initCanvas();
        this.loadAssets();
        this.setupEventListeners();
        
        // Start animation frame loop
        this.lastTime = performance.now();
        requestAnimationFrame((t) => this.tick(t));

        // Auto-initialize controls indicator
        this.updateTimelineProgress();
    }

    initDOMElements() {
        this.container = document.querySelector(".cinema-container");
        this.canvas = document.getElementById("promoCanvas");
        this.ctx = this.canvas.getContext("2d");
        
        this.playPauseBtn = document.getElementById("promoPlayPause");
        this.audioBtn = document.getElementById("promoAudioToggleBtn");
        this.floatingAudioBtn = document.getElementById("promoFloatingAudioBtn");
        this.restartBtn = document.getElementById("promoRestart");
        this.timelineWrapper = document.getElementById("promoTimelineWrapper");
        this.timelineProgress = document.getElementById("promoTimelineProgress");
        this.timeDisplay = document.getElementById("promoTimeDisplay");
        this.markerContainer = document.getElementById("promoTimelineMarkers");

        // Scene HTML layers
        this.sceneElements = this.scenes.map(s => document.querySelector(s.selector));

        // Create timeline markers dynamically
        if (this.markerContainer) {
            this.markerContainer.innerHTML = "";
            this.scenes.forEach(s => {
                const marker = document.createElement("div");
                marker.className = "promo-marker";
                marker.style.left = `${(s.start / this.duration) * 100}%`;
                marker.dataset.time = s.start;
                this.markerContainer.appendChild(marker);
            });
        }
    }

    initCanvas() {
        const resize = () => {
            const rect = this.container.getBoundingClientRect();
            this.canvas.width = rect.width;
            this.canvas.height = rect.height;
        };
        
        resize();
        window.addEventListener("resize", resize);

        // Generate floaty background dust particles
        for (let i = 0; i < this.maxParticles; i++) {
            this.particles.push({
                x: Math.random() * this.canvas.width,
                y: Math.random() * this.canvas.height,
                radius: Math.random() * 2 + 1,
                vx: (Math.random() - 0.5) * 0.4,
                vy: (Math.random() - 0.5) * 0.4 - 0.1, // slowly float upwards
                color: Math.random() > 0.5 ? "rgba(139, 92, 246, 0.4)" : "rgba(59, 130, 246, 0.4)"
            });
        }
    }

    loadAssets() {
        // Load generated background images
        this.images = {};
        const imageSources = {
            1: "/images/promo_city_bg.png",
            2: "/images/promo_students.png",
            3: "/images/promo_mobile_app.png",
            4: "/images/promo_gaming.png",
            5: "/images/promo_concert.png",
            6: "/images/promo_rewards.png",
            7: "/images/promo_logo_reveal.png"
        };

        Object.keys(imageSources).forEach(id => {
            const img = new Image();
            img.src = imageSources[id];
            img.onload = () => {
                this.images[id] = img;
            };
            img.onerror = () => {
                console.warn(`Failed to load background image ${id}, falling back to digital shader`);
            };
        });
    }

    setupEventListeners() {
        // Play / Pause toggle
        if (this.playPauseBtn) {
            this.playPauseBtn.addEventListener("click", () => this.togglePlay());
        }

        // Mute / Unmute audio
        const handleMute = () => this.toggleAudio();
        if (this.audioBtn) this.audioBtn.addEventListener("click", handleMute);
        if (this.floatingAudioBtn) this.floatingAudioBtn.addEventListener("click", handleMute);

        // Restart
        if (this.restartBtn) {
            this.restartBtn.addEventListener("click", () => this.seekTo(0.0));
        }

        // Timeline click-scrubbing
        if (this.timelineWrapper) {
            this.timelineWrapper.addEventListener("click", (e) => {
                const rect = this.timelineWrapper.getBoundingClientRect();
                const clickX = e.clientX - rect.left;
                const percentage = clickX / rect.width;
                this.seekTo(percentage * this.duration);
            });
        }

        // Connect scene nodes directly on Scene 7 clicks (redirects)
        document.querySelectorAll(".city-node").forEach(node => {
            node.addEventListener("click", (e) => {
                const target = node.getAttribute("data-link");
                if (target) {
                    window.location.href = target;
                }
            });
        });
    }

    togglePlay() {
        this.isPlaying = !this.isPlaying;
        if (this.isPlaying) {
            this.playPauseBtn.querySelector("i").className = "fas fa-pause";
            if (!this.isMuted) this.resumeSynth();
        } else {
            this.playPauseBtn.querySelector("i").className = "fas fa-play";
            if (this.audioCtx) this.audioCtx.suspend();
        }
    }

    seekTo(time) {
        this.currentTime = Math.max(0.0, Math.min(this.duration, time));
        this.coinBurstTriggered = false; // Reset burst if seeking back
        
        // Trigger poll bar animations instantly if seeking into Scene 3
        if (this.currentTime >= 3.0 && this.currentTime < 5.0) {
            this.triggerPollAnimation();
        } else {
            this.resetPollAnimation();
        }

        this.updateTimelineProgress();
        this.updateActiveScene();
        this.playTransitionSound();
    }

    toggleAudio() {
        this.isMuted = !this.isMuted;
        
        // Create audio context on first user gesture
        if (!this.audioCtx) {
            this.initAudioContext();
        }

        const iconClass = this.isMuted ? "fas fa-volume-mute" : "fas fa-volume-up";
        const textLabel = this.isMuted ? "Mute" : "Unmute";
        
        if (this.audioBtn) {
            this.audioBtn.querySelector("i").className = iconClass;
        }
        if (this.floatingAudioBtn) {
            this.floatingAudioBtn.querySelector("i").className = iconClass;
            this.floatingAudioBtn.style.borderColor = this.isMuted ? "var(--neon-purple)" : "var(--neon-pink)";
            this.floatingAudioBtn.style.boxShadow = this.isMuted ? "0 0 15px var(--neon-purple-glow)" : "0 0 15px var(--neon-pink-glow)";
        }

        if (this.isMuted) {
            if (this.gainNode) this.gainNode.gain.setTargetAtTime(0.0, this.audioCtx.currentTime, 0.1);
        } else {
            this.resumeSynth();
            if (this.gainNode) this.gainNode.gain.setTargetAtTime(0.35, this.audioCtx.currentTime, 0.1); // Master Volume
        }
    }

    /* ──── Web Audio API Synthesizer ──── */

    initAudioContext() {
        try {
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            this.audioCtx = new AudioContext();
            this.gainNode = this.audioCtx.createGain();
            this.gainNode.gain.setValueAtTime(0.0, this.audioCtx.currentTime); // start silent
            this.gainNode.connect(this.audioCtx.destination);
            
            // Start the cyber bass loop
            this.startMusicSynthesizer();
        } catch (e) {
            console.error("Web Audio API not supported", e);
        }
    }

    resumeSynth() {
        if (this.audioCtx && this.audioCtx.state === "suspended") {
            this.audioCtx.resume();
        }
    }

    startMusicSynthesizer() {
        let step = 0;
        const bpm = 125;
        const noteLength = 60 / bpm / 2; // eighth notes

        const playPattern = () => {
            if (this.isMuted || !this.isPlaying) return;
            const now = this.audioCtx.currentTime;

            // Cyberpunk bass sequence notes (A1, A1, C2, D2, A1, G1, A1, E2)
            const bassPitches = [55.0, 55.0, 65.4, 73.4, 55.0, 49.0, 55.0, 82.4];
            const activeBassHz = bassPitches[step % bassPitches.length];

            // Bass Synthesizer Node
            const osc = this.audioCtx.createOscillator();
            const osc2 = this.audioCtx.createOscillator();
            const bassGain = this.audioCtx.createGain();
            const filter = this.audioCtx.createBiquadFilter();

            osc.type = "sawtooth";
            osc2.type = "square";
            osc.frequency.setValueAtTime(activeBassHz, now);
            osc2.frequency.setValueAtTime(activeBassHz * 1.005, now); // slight detune

            filter.type = "lowpass";
            filter.frequency.setValueAtTime(200, now);
            filter.frequency.exponentialRampToValueAtTime(1200, now + 0.05);
            filter.frequency.exponentialRampToValueAtTime(100, now + noteLength);

            bassGain.gain.setValueAtTime(0.6, now);
            bassGain.gain.exponentialRampToValueAtTime(0.01, now + noteLength);

            osc.connect(filter);
            osc2.connect(filter);
            filter.connect(bassGain);
            bassGain.connect(this.gainNode);

            osc.start(now);
            osc2.start(now);
            osc.stop(now + noteLength);
            osc2.stop(now + noteLength);

            // Arpeggio Lead (plays on scene 2, 4, 7 for futuristic vibes)
            if (this.currentSceneIndex === 1 || this.currentSceneIndex === 3 || this.currentSceneIndex === 6) {
                if (step % 2 === 0) {
                    const leadPitches = [220.0, 329.6, 261.6, 440.0, 392.0];
                    const activeLeadHz = leadPitches[(step / 2) % leadPitches.length];
                    
                    const leadOsc = this.audioCtx.createOscillator();
                    const leadGain = this.audioCtx.createGain();
                    
                    leadOsc.type = "sawtooth";
                    leadOsc.frequency.setValueAtTime(activeLeadHz, now);
                    
                    leadGain.gain.setValueAtTime(0.12, now);
                    leadGain.gain.exponentialRampToValueAtTime(0.01, now + noteLength * 1.8);
                    
                    leadOsc.connect(leadGain);
                    leadGain.connect(this.gainNode);
                    
                    leadOsc.start(now);
                    leadOsc.stop(now + noteLength * 1.8);
                }
            }

            step++;
        };

        // Precision timing interval using Web Audio Scheduler
        const scheduleAheadTime = 0.1;
        let nextNoteTime = 0.0;

        const scheduler = () => {
            while (nextNoteTime < this.audioCtx.currentTime + scheduleAheadTime) {
                if (nextNoteTime === 0) nextNoteTime = this.audioCtx.currentTime;
                playPattern();
                nextNoteTime += noteLength;
            }
        };

        this.synthLoopId = setInterval(scheduler, 50);
    }

    playTransitionSound() {
        if (!this.audioCtx || this.isMuted) return;
        const now = this.audioCtx.currentTime;

        // 1. Noise Swoosh Effect
        const bufferSize = this.audioCtx.sampleRate * 0.6; // 0.6s sweep
        const buffer = this.audioCtx.createBuffer(1, bufferSize, this.audioCtx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = Math.random() * 2 - 1;
        }

        const whiteNoise = this.audioCtx.createBufferSource();
        whiteNoise.buffer = buffer;

        const filter = this.audioCtx.createBiquadFilter();
        filter.type = "bandpass";
        filter.frequency.setValueAtTime(100, now);
        filter.frequency.exponentialRampToValueAtTime(5000, now + 0.4);
        filter.Q.setValueAtTime(8, now);

        const noiseGain = this.audioCtx.createGain();
        noiseGain.gain.setValueAtTime(0.25, now);
        noiseGain.gain.exponentialRampToValueAtTime(0.001, now + 0.5);

        whiteNoise.connect(filter);
        filter.connect(noiseGain);
        noiseGain.connect(this.gainNode);

        whiteNoise.start(now);
        whiteNoise.stop(now + 0.6);
    }

    playCoinSound() {
        if (!this.audioCtx || this.isMuted) return;
        const now = this.audioCtx.currentTime;

        // Play 4 rapid coin bell sounds
        for (let i = 0; i < 4; i++) {
            const triggerTime = now + (i * 0.12);
            
            const osc = this.audioCtx.createOscillator();
            const osc2 = this.audioCtx.createOscillator();
            const gain = this.audioCtx.createGain();

            osc.type = "sine";
            osc2.type = "sine";
            
            osc.frequency.setValueAtTime(987.77, triggerTime); // B5 note
            osc2.frequency.setValueAtTime(1318.51, triggerTime); // E6 note (major harmony)

            gain.gain.setValueAtTime(0.18, triggerTime);
            gain.gain.exponentialRampToValueAtTime(0.001, triggerTime + 0.25);

            osc.connect(gain);
            osc2.connect(gain);
            gain.connect(this.gainNode);

            osc.start(triggerTime);
            osc2.start(triggerTime);
            osc.stop(triggerTime + 0.3);
            osc2.stop(triggerTime + 0.3);
        }
    }

    playFinalChime() {
        if (!this.audioCtx || this.isMuted) return;
        const now = this.audioCtx.currentTime;

        // Warm Major 7th reveal chord (Amaj7: A2, E3, A3, C#4, G#4)
        const notes = [110.0, 164.81, 220.0, 277.18, 415.3];
        
        notes.forEach((pitch, index) => {
            const osc = this.audioCtx.createOscillator();
            const gain = this.audioCtx.createGain();
            
            osc.type = "triangle";
            osc.frequency.setValueAtTime(pitch, now);
            
            // Arpeggiate slightly
            const noteStart = now + (index * 0.05);
            
            gain.gain.setValueAtTime(0.0, now);
            gain.gain.setValueAtTime(0.18, noteStart);
            gain.gain.exponentialRampToValueAtTime(0.001, noteStart + 1.8);
            
            osc.connect(gain);
            gain.connect(this.gainNode);
            
            osc.start(now);
            osc.stop(now + 2.0);
        });
    }

    /* ──── Core Player Ticks & Updates ──── */

    tick(timestamp) {
        const dt = (timestamp - this.lastTime) / 1000.0;
        this.lastTime = timestamp;

        if (this.isPlaying) {
            this.currentTime += dt;
            if (this.currentTime >= this.duration) {
                // Loop or stop
                this.currentTime = this.duration;
                this.isPlaying = false;
                if (this.playPauseBtn) this.playPauseBtn.querySelector("i").className = "fas fa-redo";
            }
            
            this.updateTimelineProgress();
            this.updateActiveScene();
        }

        // Draw visual background & animations on Canvas
        this.drawCanvasFrame();

        requestAnimationFrame((t) => this.tick(t));
    }

    updateTimelineProgress() {
        if (this.timelineProgress) {
            const pct = (this.currentTime / this.duration) * 100;
            this.timelineProgress.style.width = `${pct}%`;
        }

        if (this.timeDisplay) {
            const formatted = this.currentTime.toFixed(1);
            this.timeDisplay.textContent = `0:${formatted.replace('.', ':')} / 0:10`;
        }

        // Highlight markers
        document.querySelectorAll(".promo-marker").forEach(marker => {
            const markTime = parseFloat(marker.dataset.time);
            if (this.currentTime >= markTime) {
                marker.classList.add("active");
            } else {
                marker.classList.remove("active");
            }
        });
    }

    updateActiveScene() {
        let activeIdx = 0;
        for (let i = 0; i < this.scenes.length; i++) {
            if (this.currentTime >= this.scenes[i].start && this.currentTime < this.scenes[i].end) {
                activeIdx = i;
                break;
            }
        }
        
        // Handles end-of-video state
        if (this.currentTime >= this.duration) {
            activeIdx = this.scenes.length - 1;
        }

        if (activeIdx !== this.currentSceneIndex) {
            this.currentSceneIndex = activeIdx;
            
            // Switch DOM layout classes
            this.sceneElements.forEach((el, idx) => {
                if (idx === activeIdx) {
                    el.classList.add("active");
                } else {
                    el.classList.remove("active");
                }
            });

            // Play transition swoosh
            this.playTransitionSound();

            // Trigger specific interactive scene routines
            if (activeIdx === 2) {
                this.triggerPollAnimation();
            } else {
                this.resetPollAnimation();
            }

            if (activeIdx === 5 && !this.coinBurstTriggered) {
                this.triggerCoinBurst();
            }

            if (activeIdx === 6) {
                this.playFinalChime();
            }
        }
    }

    /* ──── Interactive Scene Routines ──── */

    triggerPollAnimation() {
        const fills = document.querySelectorAll(".promo-poll-fill");
        // Values: Music Festival: 88%, Gaming: 76%, AI Workshop: 92%, Tech Meetup: 62%
        const targets = [88, 76, 92, 62];
        
        fills.forEach((fill, index) => {
            // Animate only if within scene timeline
            fill.style.width = `${targets[index]}%`;
        });
    }

    resetPollAnimation() {
        document.querySelectorAll(".promo-poll-fill").forEach(fill => {
            fill.style.width = "0%";
        });
    }

    triggerCoinBurst() {
        this.coinBurstTriggered = true;
        this.playCoinSound();
        
        const centerX = this.canvas.width / 2;
        const centerY = this.canvas.height / 2;
        
        // Spawn 40 bouncy coins
        for (let i = 0; i < 45; i++) {
            const angle = Math.random() * Math.PI * 2;
            const velocity = Math.random() * 8 + 4;
            
            this.coins.push({
                x: centerX,
                y: centerY,
                vx: Math.cos(angle) * velocity,
                vy: Math.sin(angle) * velocity - 3, // eject upwards
                radius: Math.random() * 10 + 6,
                rotation: Math.random() * Math.PI,
                rotVelocity: (Math.random() - 0.5) * 0.2,
                bounceCount: 0,
                color: `hsl(${Math.random() * 15 + 40}, 100%, ${Math.random() * 20 + 50}%)` // Golden shades
            });
        }
    }

    /* ──── Canvas Painting ──── */

    drawCanvasFrame() {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);

        // 1. Draw Active Image with camera panning/zooming
        const activeImg = this.images[this.currentSceneIndex + 1];
        if (activeImg) {
            this.ctx.save();
            
            // Compute camera zoom scale based on timing
            let scale = 1.02;
            let dx = 0;
            let dy = 0;
            const scene = this.scenes[this.currentSceneIndex];
            const progress = (this.currentTime - scene.start) / (scene.end - scene.start);

            if (this.currentSceneIndex === 0) {
                // Scene 1: Zoom inwards
                scale = 1.0 + (progress * 0.05);
            } else if (this.currentSceneIndex === 1) {
                // Scene 2: Pan horizontally
                dx = (progress - 0.5) * 20;
            } else if (this.currentSceneIndex === 3) {
                // Scene 4: Gaming shake
                const shakeIntensity = 4;
                dx = (Math.random() - 0.5) * shakeIntensity;
                dy = (Math.random() - 0.5) * shakeIntensity;
            } else if (this.currentSceneIndex === 6) {
                // Scene 7: Reveal Zoom Out
                scale = 1.06 - (progress * 0.06);
            }

            // Apply transforms centered on canvas
            this.ctx.translate(this.canvas.width / 2, this.canvas.height / 2);
            this.ctx.scale(scale, scale);
            this.ctx.translate(-this.canvas.width / 2 + dx, -this.canvas.height / 2 + dy);

            // Draw image covering the canvas
            this.drawCoverImage(activeImg);
            this.ctx.restore();
        } else {
            // Draw digital fallback shader grid
            this.drawFallbackGrid();
        }

        // 2. Draw Spotlight beams in Scene 5 (Concert)
        if (this.currentSceneIndex === 4) {
            this.drawConcertSpotlights();
        }

        // 3. Update & Draw floaty background dust particles
        this.ctx.save();
        this.particles.forEach(p => {
            p.x += p.vx;
            p.y += p.vy;

            // wrap around canvas
            if (p.x < 0) p.x = this.canvas.width;
            if (p.x > this.canvas.width) p.x = 0;
            if (p.y < 0) p.y = this.canvas.height;
            if (p.y > this.canvas.height) p.y = this.canvas.height;

            this.ctx.beginPath();
            this.ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
            this.ctx.fillStyle = p.color;
            this.ctx.shadowBlur = 8;
            this.ctx.shadowColor = p.color;
            this.ctx.fill();
        });
        this.ctx.restore();

        // 4. Update & Draw Gold Coins physics particles (Scene 6)
        if (this.currentSceneIndex === 5 && this.coins.length > 0) {
            this.updateAndDrawCoins();
        }
    }

    drawCoverImage(img) {
        const imgRatio = img.width / img.height;
        const canvasRatio = this.canvas.width / this.canvas.height;
        let w, h, x, y;

        if (imgRatio > canvasRatio) {
            h = this.canvas.height;
            w = h * imgRatio;
            x = (this.canvas.width - w) / 2;
            y = 0;
        } else {
            w = this.canvas.width;
            h = w / imgRatio;
            x = 0;
            y = (this.canvas.height - h) / 2;
        }

        this.ctx.drawImage(img, x, y, w, h);
    }

    drawFallbackGrid() {
        // Draw neon space gradient
        const grad = this.ctx.createLinearGradient(0, 0, 0, this.canvas.height);
        grad.addColorStop(0, "#05050e");
        grad.addColorStop(1, "#0a0a22");
        this.ctx.fillStyle = grad;
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

        // Draw cyberpunk grid
        this.ctx.strokeStyle = "rgba(139, 92, 246, 0.15)";
        this.ctx.lineWidth = 1;
        const gridSize = 40;
        
        for (let x = 0; x < this.canvas.width; x += gridSize) {
            this.ctx.beginPath();
            this.ctx.moveTo(x, 0);
            this.ctx.lineTo(x, this.canvas.height);
            this.ctx.stroke();
        }
        for (let y = 0; y < this.canvas.height; y += gridSize) {
            this.ctx.beginPath();
            this.ctx.moveTo(0, y);
            this.ctx.lineTo(this.canvas.width, y);
            this.ctx.stroke();
        }
    }

    drawConcertSpotlights() {
        const now = performance.now() / 1000;
        this.ctx.save();
        this.ctx.globalCompositeOperation = "screen";

        const drawBeam = (sourceX, color) => {
            const angle = Math.sin(now * 1.5 + sourceX) * 0.4 + Math.PI / 2; // sweep oscillating
            const targetLength = this.canvas.height * 1.2;
            const targetX = sourceX + Math.cos(angle) * targetLength;
            const targetY = Math.sin(angle) * targetLength;

            const gradient = this.ctx.createRadialGradient(
                sourceX, this.canvas.height, 0, 
                sourceX, this.canvas.height, this.canvas.width * 0.3
            );
            gradient.addColorStop(0, color);
            gradient.addColorStop(1, "transparent");

            this.ctx.beginPath();
            this.ctx.moveTo(sourceX - 30, this.canvas.height);
            this.ctx.lineTo(sourceX + 30, this.canvas.height);
            this.ctx.lineTo(targetX + 150, targetY);
            this.ctx.lineTo(targetX - 150, targetY);
            this.ctx.closePath();
            this.ctx.fillStyle = gradient;
            this.ctx.fill();
        };

        drawBeam(this.canvas.width * 0.25, "rgba(236, 72, 153, 0.25)");
        drawBeam(this.canvas.width * 0.75, "rgba(59, 130, 246, 0.25)");
        
        this.ctx.restore();
    }

    updateAndDrawCoins() {
        this.ctx.save();
        const gravity = 0.4;
        const bounceDamping = -0.55;

        this.coins.forEach(c => {
            // Apply physics
            c.vy += gravity;
            c.x += c.vx;
            c.y += c.vy;
            c.rotation += c.rotVelocity;

            // Bounce on bottom floor
            const floor = this.canvas.height - 30;
            if (c.y > floor) {
                c.y = floor;
                c.vy *= bounceDamping;
                c.vx *= 0.8; // friction
                c.bounceCount++;
            }

            // Draw coin (3D rotating gold disk)
            this.ctx.save();
            this.ctx.translate(c.x, c.y);
            this.ctx.rotate(c.rotation);
            this.ctx.scale(Math.abs(Math.sin(c.rotation * 2)) + 0.1, 1); // spin compression effect

            // Outermost glowing shadow
            this.ctx.beginPath();
            this.ctx.arc(0, 0, c.radius, 0, Math.PI * 2);
            this.ctx.fillStyle = "#F59E0B";
            this.ctx.shadowBlur = 10;
            this.ctx.shadowColor = "rgba(245, 158, 11, 0.8)";
            this.ctx.fill();

            // Inner coin details
            this.ctx.beginPath();
            this.ctx.arc(0, 0, c.radius * 0.8, 0, Math.PI * 2);
            this.ctx.fillStyle = "#FBBF24";
            this.ctx.fill();

            // Centered dollar/coin logo
            this.ctx.font = `bold ${c.radius}px 'Outfit'`;
            this.ctx.fillStyle = "#B45309";
            this.ctx.textAlign = "center";
            this.ctx.textBaseline = "middle";
            this.ctx.fillText("Z", 0, 0);

            this.ctx.restore();
        });

        // Filter out dead/lost coins off left/right edges
        this.coins = this.coins.filter(c => c.x > -50 && c.x < this.canvas.width + 50 && c.bounceCount < 5);
        this.ctx.restore();
    }
}

// Instantiate player upon DOM content load
document.addEventListener("DOMContentLoaded", () => {
    // Only target the main homepage that has the promo wrapper element
    if (document.querySelector(".cinema-container")) {
        window.zentrixPlayer = new ZentrixPromoPlayer();
    }
});
