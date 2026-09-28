import { afterEach, describe, expect, it, vi } from "vitest"
import { apiClient, studentsApiClient } from "./api-client"

const jsonResponse = (status: number, body: unknown) =>
    new Response(body === undefined ? null : JSON.stringify(body), {
        status,
        headers: { "content-type": "application/json" },
    })

describe("api-client", () => {
    afterEach(() => {
        vi.unstubAllGlobals()
    })

    it("chama a library-api pelo prefixo do gateway", async () => {
        const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, [{ id: 1 }]))
        vi.stubGlobal("fetch", fetchMock)

        const books = await apiClient<{ id: number }[]>("/api/books")

        expect(fetchMock).toHaveBeenCalledWith("/library-api/api/books", expect.anything())
        expect(books).toEqual([{ id: 1 }])
    })

    it("chama a students-api pelo prefixo do gateway e envia o corpo em JSON", async () => {
        const fetchMock = vi.fn().mockResolvedValue(jsonResponse(201, { id: 5 }))
        vi.stubGlobal("fetch", fetchMock)

        await studentsApiClient("/api/students", { method: "POST", body: { name: "Carla" } })

        const [url, init] = fetchMock.mock.calls[0]
        expect(url).toBe("/students-api/api/students")
        expect(init.method).toBe("POST")
        expect(init.body).toBe(JSON.stringify({ name: "Carla" }))
        expect(init.headers["Content-Type"]).toBe("application/json")
    })

    it("não tenta ler corpo em respostas 204", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 204 })))

        await expect(apiClient("/api/loans/1", { method: "DELETE" })).resolves.toBeUndefined()
    })

    it("usa a mensagem de erro do back-end", async () => {
        vi.stubGlobal("fetch", vi.fn().mockResolvedValue(
            jsonResponse(409, { status: 409, message: "O estudante esta com situacao TRANCADO" }),
        ))

        await expect(apiClient("/api/loans", { method: "POST", body: {} }))
            .rejects.toThrow("O estudante esta com situacao TRANCADO")
    })
})
