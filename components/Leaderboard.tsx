"use client";

import { useEffect, useState } from "react";

export function Leaderboard() {
  const [users, setUsers] = useState<{ name: string; rating: number }[]>([]);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await fetch("/api/leaderboard");
        if (res.ok) {
          const data = await res.json();
          if (Array.isArray(data)) {
            const arr: { name: string; rating: number }[] = [];
            for (let i = 0; i < data.length; i += 2) {
              arr.push({ name: data[i], rating: parseInt(data[i + 1]) });
            }
            setUsers(arr);
            return;
          }
        }
      } catch (e) {
        console.error("Failed to load remote leaderboard", e);
      }
      if (typeof window !== "undefined") {
        const local = Object.entries(localStorage)
          .filter(([key]) => key.startsWith("rating_"))
          .map(([key, value]) => ({ name: key.replace("rating_", ""), rating: parseInt(value) }))
          .sort((a, b) => b.rating - a.rating);
        setUsers(local);
      }
    };
    load();
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
