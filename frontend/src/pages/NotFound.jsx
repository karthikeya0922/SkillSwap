import { Compass } from 'lucide-react'
import Button from '../components/ui/Button'
import Logo from '../components/Logo'

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center px-4 text-center">
      <Logo />
      <p className="mt-10 text-7xl font-extrabold text-brand-100">404</p>
      <h1 className="mt-2 text-2xl font-bold text-slate-900">This page wandered off</h1>
      <p className="mt-2 max-w-sm text-slate-500">The page you're looking for doesn't exist or has moved.</p>
      <Button to="/" icon={Compass} className="mt-6">
        Back to SkillSwap
      </Button>
    </div>
  )
}
