/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 后端 API 的绝对地址前缀；留空表示与页面同源 */
  readonly VITE_API_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
