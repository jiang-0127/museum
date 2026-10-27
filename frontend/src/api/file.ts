import { post } from './http'

export interface UploadResult {
  url: string
  fileName: string
  size: number
}

export function uploadImage(file: File) {
  const data = new FormData()
  data.append('file', file)
  return post<UploadResult>('/files/upload', data)
}
