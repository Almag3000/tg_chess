"use client";

export function Leaderboard() {
  const users = typeof window !== "undefined"
    ? Object.entries(localStorage)
      .filter(([key]) => key.startsWith("rating_"))
      .map(([key, value]) => ({ name: key.replace("rating_", ""), rating: parseInt(value) }))
      .sort((a, b) => b.rating - a.rating)
    : [];

  return (
    <div className="mt-4">
      <h3 className="text-lg mb-1">Таблица лидеров</h3>
      <ul>
        {users.map((u, i) => (
          <li key={i}>
            {i + 1}. {u.name}: {u.rating}
          </li>
        ))}
      </ul>
    </div>
  );
}
