import React from 'react'
import ReactDOM from 'react-dom/client'
import { GoogleOAuthProvider } from '@react-oauth/google'
import App from './App.jsx'

/* 디자인 시스템 (토큰·레이아웃·유틸리티) — 참조 TeamFlow UI */
import './styles/global.css'
import './styles/layout.css'
import './styles/buttons.css'
import './styles/cards.css'
import './styles/stat.css'
import './styles/badge.css'
import './styles/task.css'
import './styles/forms.css'
import './styles/modal.css'
import './styles/member.css'
import './styles/timeline.css'
import './styles/utils.css'
import './styles/animations.css'

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || ''

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <App />
    </GoogleOAuthProvider>
  </React.StrictMode>,
)
