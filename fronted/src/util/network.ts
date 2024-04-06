const base_url = "http://localhost:8080"

export const request = async (url: string, init: RequestInit = {}) => {
    return await fetch(`${base_url}/${url}`, init).then(res => res.json())
}