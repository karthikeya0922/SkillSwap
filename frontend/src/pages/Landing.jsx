import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight, ArrowLeftRight, Coins, Handshake, Search, ShieldCheck, Sparkles, Star, UserRoundPen, Zap } from 'lucide-react'
import Logo from '../components/Logo'
import DynamicIcon from '../components/DynamicIcon'
import { skillService } from '../services'
import { useAuth } from '../context/AuthContext'

const STEPS = [
  { icon: UserRoundPen, title: 'Build your skill profile', text: 'List what you can teach and what you want to learn, with your level and availability.' },
  { icon: Search, title: 'Find your match', text: 'Our matching engine finds two-way swaps — you teach Java, they teach UI/UX.' },
  { icon: ArrowLeftRight, title: 'Exchange knowledge', text: 'Chat, book a session and learn together. One hour taught earns one Skill Credit.' },
  { icon: Star, title: 'Earn reputation', text: 'Collect ratings, XP and badges as you help others — from Beginner to Community Master.' },
]

// Placeholder marketing numbers for the landing page.
const STATS = [
  ['1,200+', 'Students'],
  ['450+', 'Skills'],
  ['3,800+', 'Sessions'],
  ['4.8', 'Average rating'],
]

const FALLBACK_CATEGORIES = [
  ['Programming', 'Code2', '#6366f1'],
  ['Web Development', 'Globe', '#0ea5e9'],
  ['UI/UX', 'PenTool', '#ec4899'],
  ['Design', 'Palette', '#f97316'],
  ['Music', 'Music', '#f43f5e'],
  ['Video Editing', 'Clapperboard', '#a855f7'],
  ['Photography', 'Camera', '#64748b'],
  ['Business', 'Briefcase', '#0f766e'],
].map(([name, icon, color]) => ({ name, icon, color, skillCount: null }))

function HeroVisual() {
  return (
    <div className="relative mx-auto w-full max-w-md">
      <div className="absolute -inset-6 rounded-[2rem] bg-gradient-to-br from-brand-400/30 via-violet-400/20 to-orange-300/20 blur-2xl" />
      <div className="relative card space-y-4 p-5">
        <div className="flex items-center justify-between">
          <span className="chip bg-orange-50 font-semibold text-orange-700">🔥 Great SkillSwap Match</span>
          <span className="text-2xl font-extrabold text-orange-600">92%</span>
        </div>
        <div className="grid grid-cols-2 gap-3">
          {[
            ['You', 'Java', 'UI/UX', 'from-indigo-500 to-violet-500', 'YO'],
            ['Rahul', 'UI/UX', 'Java', 'from-rose-500 to-pink-400', 'RV'],
          ].map(([name, teaches, wants, gradient, init]) => (
            <div key={name} className="rounded-2xl bg-slate-50 p-3">
              <div className={`mb-2 grid h-10 w-10 place-items-center rounded-full bg-gradient-to-br ${gradient} text-xs font-bold text-white`}>{init}</div>
              <p className="text-sm font-semibold text-slate-900">{name}</p>
              <p className="mt-1 text-xs text-slate-500">
                Teaches <b className="text-slate-700">{teaches}</b>
              </p>
              <p className="text-xs text-slate-500">
                Wants <b className="text-slate-700">{wants}</b>
              </p>
            </div>
          ))}
        </div>
        <p className="rounded-xl bg-brand-50 p-3 text-xs leading-relaxed text-brand-900">
          You want to learn UI/UX and Rahul teaches UI/UX. Rahul wants to learn Java and you teach Java.
        </p>
        <div className="flex items-center justify-between rounded-xl border border-slate-100 p-3">
          <div className="flex items-center gap-2 text-sm">
            <Coins size={16} className="text-amber-500" />
            <span className="font-semibold">+2 credits</span>
            <span className="text-slate-500">· Java teaching session</span>
          </div>
          <span className="chip bg-emerald-50 text-emerald-700">Completed</span>
        </div>
      </div>
    </div>
  )
}

export default function Landing() {
  const { isAuthenticated, isAdmin } = useAuth()
  const [categories, setCategories] = useState(FALLBACK_CATEGORIES)

  useEffect(() => {
    skillService
      .categories()
      .then((list) => list.length && setCategories(list))
      .catch(() => {})
  }, [])

  const home = isAdmin ? '/admin' : '/dashboard'

  return (
    <div className="min-h-screen bg-white">
      <header className="sticky top-0 z-20 border-b border-slate-100 bg-white/80 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-4 sm:px-6">
          <Logo />
          <nav className="hidden items-center gap-7 text-sm font-medium text-slate-600 md:flex">
            <a href="#how" className="hover:text-slate-900">How it works</a>
            <a href="#skills" className="hover:text-slate-900">Skills</a>
            <a href="#credits" className="hover:text-slate-900">Time credits</a>
          </nav>
          <div className="flex items-center gap-2">
            {isAuthenticated ? (
              <Link to={home} className="btn btn-primary">Open app</Link>
            ) : (
              <>
                <Link to="/login" className="btn btn-ghost">Sign in</Link>
                <Link to="/register" className="btn btn-primary">Get Started</Link>
              </>
            )}
          </div>
        </div>
      </header>

      <section className="relative overflow-hidden">
        <div className="absolute inset-0 -z-10 bg-[radial-gradient(ellipse_at_top_left,_var(--color-brand-100),_transparent_55%),radial-gradient(ellipse_at_bottom_right,_#ffedd5,_transparent_50%)]" />
        <div className="mx-auto grid max-w-6xl items-center gap-12 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:py-24">
          <div>
            <span className="chip mb-5 bg-white text-brand-700 shadow-sm ring-1 ring-brand-100">
              <Sparkles size={13} /> Peer learning for college students
            </span>
            <h1 className="text-4xl leading-[1.1] font-extrabold tracking-tight text-slate-900 sm:text-5xl lg:text-6xl">
              Learn from your peers. <span className="bg-gradient-to-r from-brand-600 to-violet-600 bg-clip-text text-transparent">Share what you know.</span>
            </h1>
            <p className="mt-5 max-w-xl text-lg text-slate-600">
              SkillSwap connects students who want to learn with students who can teach — powered by skills, time, and community.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to={isAuthenticated ? home : '/register'} className="btn btn-primary btn-lg">
                Get Started <ArrowRight size={18} />
              </Link>
              <a href="#skills" className="btn btn-secondary btn-lg">Explore Skills</a>
            </div>
            <div className="mt-8 flex flex-wrap items-center gap-x-6 gap-y-2 text-sm text-slate-500">
              <span className="inline-flex items-center gap-1.5"><ShieldCheck size={16} className="text-emerald-500" /> Free for students</span>
              <span className="inline-flex items-center gap-1.5"><Zap size={16} className="text-amber-500" /> No money, just time</span>
              <span className="inline-flex items-center gap-1.5"><Handshake size={16} className="text-brand-500" /> Real-time chat</span>
            </div>
          </div>
          <HeroVisual />
        </div>
      </section>

      <section className="border-y border-slate-100 bg-slate-50/60">
        <div className="mx-auto grid max-w-6xl grid-cols-2 gap-6 px-4 py-10 sm:px-6 md:grid-cols-4">
          {STATS.map(([value, label]) => (
            <div key={label} className="text-center">
              <p className="text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl">{value}</p>
              <p className="mt-1 text-sm text-slate-500">{label}</p>
            </div>
          ))}
        </div>
      </section>

      <section id="how" className="mx-auto max-w-6xl scroll-mt-20 px-4 py-20 sm:px-6">
        <div className="mx-auto max-w-2xl text-center">
          <p className="text-sm font-semibold tracking-wider text-brand-600 uppercase">How it works</p>
          <h2 className="mt-2 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">From profile to progress in four steps</h2>
        </div>
        <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {STEPS.map((step, i) => (
            <div key={step.title} className="card card-hover p-6">
              <div className="flex items-center justify-between">
                <div className="grid h-11 w-11 place-items-center rounded-xl bg-brand-50 text-brand-600">
                  <step.icon size={20} />
                </div>
                <span className="text-3xl font-extrabold text-slate-100">0{i + 1}</span>
              </div>
              <h3 className="mt-4 font-semibold text-slate-900">{step.title}</h3>
              <p className="mt-1.5 text-sm leading-relaxed text-slate-500">{step.text}</p>
            </div>
          ))}
        </div>
      </section>

      <section id="skills" className="scroll-mt-20 bg-slate-50/60 py-20">
        <div className="mx-auto max-w-6xl px-4 sm:px-6">
          <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
            <div>
              <p className="text-sm font-semibold tracking-wider text-brand-600 uppercase">Popular skills</p>
              <h2 className="mt-2 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Whatever you're into, someone's teaching it</h2>
            </div>
            <Link to="/register" className="link inline-flex items-center gap-1 text-sm">
              Join to explore all skills <ArrowRight size={14} />
            </Link>
          </div>
          <div className="mt-10 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
            {categories.map((c) => (
              <div key={c.name} className="card card-hover flex items-center gap-3 p-4">
                <div className="grid h-11 w-11 shrink-0 place-items-center rounded-xl text-white" style={{ background: c.color }}>
                  <DynamicIcon name={c.icon} size={20} />
                </div>
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-slate-900">{c.name}</p>
                  {c.skillCount != null && <p className="text-xs text-slate-500">{c.skillCount} skills</p>}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section id="credits" className="mx-auto grid max-w-6xl scroll-mt-20 items-center gap-10 px-4 py-20 sm:px-6 lg:grid-cols-2">
        <div>
          <p className="text-sm font-semibold tracking-wider text-brand-600 uppercase">Time credits</p>
          <h2 className="mt-2 text-3xl font-bold tracking-tight text-slate-900 sm:text-4xl">Every hour counts the same</h2>
          <p className="mt-4 text-slate-600">
            Teach for an hour, earn one Skill Credit. Spend it learning anything from anyone. Credits are held safely when a session is
            accepted and released to the teacher once it's completed — cancel in time and you're refunded.
          </p>
        </div>
        <div className="card divide-y divide-slate-100">
          {[
            ['+2', 'Java Teaching Session', 'text-emerald-600'],
            ['-1', 'UI/UX Learning Session', 'text-rose-600'],
            ['+1.5', 'React Teaching Session', 'text-emerald-600'],
            ['+5', 'Welcome bonus — starter credits', 'text-emerald-600'],
          ].map(([amount, label, color]) => (
            <div key={label} className="flex items-center justify-between px-5 py-4">
              <span className="text-sm text-slate-700">{label}</span>
              <span className={`font-bold ${color}`}>{amount}</span>
            </div>
          ))}
          <div className="flex items-center justify-between bg-slate-50/80 px-5 py-4">
            <span className="text-sm font-semibold text-slate-900">Balance</span>
            <span className="inline-flex items-center gap-1.5 font-bold text-amber-700">
              <Coins size={16} /> 7.5 credits
            </span>
          </div>
        </div>
      </section>

      <section className="px-4 pb-20 sm:px-6">
        <div className="mx-auto max-w-6xl overflow-hidden rounded-3xl bg-gradient-to-br from-brand-600 via-brand-700 to-violet-700 px-6 py-14 text-center text-white sm:px-12">
          <h2 className="text-3xl font-bold tracking-tight sm:text-4xl">Start your SkillSwap journey.</h2>
          <p className="mx-auto mt-3 max-w-xl text-brand-100">Learn. Teach. Exchange. Grow. Join your campus community today — it takes two minutes.</p>
          <Link to={isAuthenticated ? home : '/register'} className="btn btn-lg mt-8 bg-white text-brand-700 hover:bg-brand-50">
            Create your free account <ArrowRight size={18} />
          </Link>
        </div>
      </section>

      <footer className="border-t border-slate-100">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-3 px-4 py-8 text-sm text-slate-500 sm:flex-row sm:px-6">
          <Logo />
          <p>Learn. Teach. Exchange. Grow. · © {new Date().getFullYear()} SkillSwap</p>
        </div>
      </footer>
    </div>
  )
}
