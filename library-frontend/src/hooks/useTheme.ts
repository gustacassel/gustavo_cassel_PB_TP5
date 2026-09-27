import { useEffect, useState } from "react"

export type Theme = "light" | "dark"

const STORAGE_KEY = "aurora-theme"

const initialTheme = (): Theme => {
    const current = document.documentElement.getAttribute("data-bs-theme")
    return current === "dark" ? "dark" : "light"
}

export function useTheme() {
    const [theme, setTheme] = useState<Theme>(initialTheme)

    useEffect(() => {
        document.documentElement.setAttribute("data-bs-theme", theme)
        try {
            localStorage.setItem(STORAGE_KEY, theme)
        } catch {
            // sem localStorage o tema so nao fica salvo
        }
    }, [theme])

    const toggleTheme = () => setTheme((value) => (value === "dark" ? "light" : "dark"))

    return { theme, toggleTheme }
}
