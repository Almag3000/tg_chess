import { createClient } from '@supabase/supabase-js'

const supabaseUrl = 'https://ahgaimhmhdwflyutjevi.supabase.co'
const supabaseAnonKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFoZ2FpbWhtaGR3Zmx5dXRqZXZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDg4OTIwMDAsImV4cCI6MjA2NDQ2ODAwMH0.HntCl1xpz0u20NEuqmittd4OVbBgAl-YBFoB2yr99pw'

export const supabase = createClient(supabaseUrl, supabaseAnonKey)