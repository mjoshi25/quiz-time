import { useEffect, useState } from "react";

const SEASONS = [
  { id: "vasanta", name: "Vasanta", english: "Spring", particles: ["🌸", "🌼", "🍃", "🦋", "🌷", "✨", "🌺", "🍀"] },
  { id: "grishma", name: "Grishma", english: "Summer", particles: ["☀️", "✨", "🌞", "✦", "🌼", "💫", "☀", "✧"] },
  { id: "varsha", name: "Varsha", english: "Monsoon", particles: ["💧", "☔", "💦", "🌧️", "💧", "✦", "💦", "☁️"] },
  { id: "sharad", name: "Sharad", english: "Autumn", particles: ["🍂", "🍁", "🍂", "🍁", "✨", "🍃", "🍂", "🍁"] },
  { id: "hemanta", name: "Hemanta", english: "Pre-winter", particles: ["🌾", "🍃", "✨", "🍂", "🌿", "✦", "🌾", "🍃"] },
  { id: "shishira", name: "Shishira", english: "Winter", particles: ["❄", "❅", "❄️", "✧", "❆", "❄", "❅", "❆"] },
];

const PARTICLE_POSITIONS = [7, 18, 29, 41, 53, 64, 76, 89, 13, 35, 58, 83];

/** Seasonal ambience for the public landing page. Changes season every 10 seconds. */
export default function SeasonAnimation() {
  const [seasonIndex, setSeasonIndex] = useState(0);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setSeasonIndex((current) => (current + 1) % SEASONS.length);
    }, 10_000);
    return () => window.clearInterval(timer);
  }, []);

  const season = SEASONS[seasonIndex];
  const particles = Array.from({ length: 12 }, (_, index) => ({
    symbol: season.particles[index % season.particles.length],
    left: PARTICLE_POSITIONS[index],
    delay: (index % 6) * -1.35,
    duration: 8.5 + (index % 4) * 1.1,
    size: 12 + ((index * 7) % 11),
    opacity: 0.2 + ((index % 4) * 0.08),
  }));

  return (
    <div className={`season-scene season-${season.id}`} data-season={season.id} aria-hidden="true">
      <div className="season-glow" />
      <div className="season-particles" key={season.id}>
        {particles.map((particle, index) => (
          <span
            className={`season-particle season-particle-${index % 4}`}
            key={`${season.id}-${index}`}
            style={{
              left: `${particle.left}%`,
              animationDelay: `${particle.delay}s`,
              animationDuration: `${particle.duration}s`,
              fontSize: `${particle.size}px`,
              opacity: particle.opacity,
            }}
          >
            {particle.symbol}
          </span>
        ))}
      </div>
      <div className="season-label" aria-hidden="true">
        <span className="season-label-dot" />
        <span>{season.name}</span>
        <span className="season-label-divider">·</span>
        <span>{season.english}</span>
      </div>
    </div>
  );
}
