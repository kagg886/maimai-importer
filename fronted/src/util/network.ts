const base_url = "localhost:8080"

export const request = async (url: string, init: RequestInit = {}) => {
    return await fetch(`http://${base_url}/${url}`, init).then(res => res.json())
}

export type Listener = {
    onLogger: (log: LogMessage) => void
    onImageGenerated: (img: string) => void
    onConfigWantToGet: (content: RequireMessage["msg"]) => void
}

export const createSocket = (listener: Listener) => {
    const ws = new WebSocket(`ws://${base_url}/ws`)
    ws.onmessage = (ev: MessageEvent<string>) => {
        console.log('receive msg:', ev.data)
        const s = JSON.parse(ev.data)
        const pack = new DataPack(s.clazz, s.data)

        switch (pack.clazz) {
            case "top.kagg886.maimai.data.LogMessage":
                const log = parseContent<LogMessage>(pack)
                log.time = new Date()
                listener.onLogger(log)
                break
            case "top.kagg886.maimai.data.WechatShowImage":
                const img = parseContent<WechatShowImage>(pack)
                if (img.uid == null) {
                    listener.onImageGenerated("")
                    return
                }
                listener.onImageGenerated(`http://${base_url}/img?id=${img.uid}`)
                break
            case "top.kagg886.maimai.data.RequireMessage":
                const msg = parseContent<RequireMessage>(pack)
                listener.onConfigWantToGet(msg.msg)
                break

        }
    }
    return ws
}

export class DataPack {
    clazz: string
    data: string

    constructor(clazz: string, data: string) {
        this.clazz = clazz
        this.data = data
    }
}

export interface LogMessage {
    level: number
    msg: string
    time: Date
}

export type WechatShowImage = {
    uid: string
}

export type RequireMessage = {
    msg: 'PROTOCOL_CONFIG'
}

export function parseContent<T>(pack: DataPack): T {
    return JSON.parse(pack.data)
}