"use client";

import { useEffect, useState } from "react";

export function Leaderboard() {
  const [users, setUsers] = useState<{ name: string; rating: number }[]>([]);

  useEffect(() => {
    fetch("/api/rating")
      .then((res) => res.json())
      .then((data) => {
        setUsers(data.leaderboard || []);
      })
      .catch(() => {});
  }, []);

  return (
    <div className="mt-4 leaderboard">
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
