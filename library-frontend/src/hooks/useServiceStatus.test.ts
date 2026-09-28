import { renderHook, waitFor } from "@testing-library/react"
import { afterEach, describe, expect, it, vi } from "vitest"
import { useServiceStatus } from "./useServiceStatus"

describe("useServiceStatus", () => {
    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it("marca os dois serviços como online quando respondem", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("{}", { status: 200 })))

        const { result } = renderHook(() => useServiceStatus())

        await waitFor(() => expect(result.current).toEqual({ library: "online", students: "online" }))
    })

    it("marca a students-api como offline quando ela não responde", async () => {
        vi.stubGlobal("fetch", vi.fn((url: string) =>
            url.startsWith("/students-api")
                ? Promise.reject(new TypeError("Failed to fetch"))
                : Promise.resolve(new Response("{}", { status: 200 })),
        ))

        const { result } = renderHook(() => useServiceStatus())

        await waitFor(() => expect(result.current).toEqual({ library: "online", students: "offline" }))
    })

    it("considera offline quando o gateway devolve erro", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response("", { status: 500 })))

        const { result } = renderHook(() => useServiceStatus())

        await waitFor(() => expect(result.current.students).toBe("offline"))
    })
})
