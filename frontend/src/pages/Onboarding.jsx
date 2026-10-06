import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowLeft, ArrowRight, Check, GraduationCap, Sparkles, UserRound } from 'lucide-react'
import Logo from '../components/Logo'
import Button from '../components/ui/Button'
import ProfileBasicsForm from '../components/ProfileBasicsForm'
import SkillEditor from '../components/SkillEditor'
import { FullPageSpinner, ErrorState } from '../components/ui/Feedback'
import { useAsync } from '../hooks/useAsync'
import { userService } from '../services'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

const STEPS = [
  { title: 'About you', icon: UserRound },
  { title: 'Skills you teach', icon: GraduationCap },
  { title: 'Skills to learn', icon: Sparkles },
]

export default function Onboarding() {
  const navigate = useNavigate()
  const toast = useToast()
  const { user, patchUser } = useAuth()
  const [step, setStep] = useState(0)
  const [finishing, setFinishing] = useState(false)
  const { data: profile, loading, error, reload, setData } = useAsync(() => userService.myProfile(), [])

  if (loading) return <FullPageSpinner />
  if (error) return <div className="p-6"><ErrorState message={error} onRetry={reload} /></div>

  const finish = async () => {
    if (!profile.teachSkills.length && !profile.learnSkills.length) {
      toast.error('Add at least one skill', 'Matches are based on the skills you teach and want to learn.')
      return
    }
    setFinishing(true)
    try {
      await userService.updateProfile({
        fullName: profile.fullName,
        college: profile.college,
        department: profile.department,
        yearOfStudy: profile.yearOfStudy,
        bio: profile.bio,
        availability: profile.availability,
        completeOnboarding: true,
      })
      patchUser({ profileCompleted: true })
      toast.success('Your profile is ready!', 'Here are your first skill matches.')
      navigate('/dashboard', { replace: true })
    } catch (e) {
      toast.error('Could not finish setup', e.message)
    } finally {
      setFinishing(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="flex h-16 items-center justify-between border-b border-slate-200/70 bg-white px-4 sm:px-8">
        <Logo />
        <button onClick={() => navigate('/dashboard')} className="text-sm font-medium text-slate-500 hover:text-slate-800">
          Skip for now
        </button>
      </header>
      <main className="mx-auto max-w-3xl px-4 py-8 sm:py-12">
        <p className="text-sm font-semibold text-brand-600">Welcome, {user?.fullName?.split(' ')[0]} 👋</p>
        <h1 className="mt-1 text-3xl font-bold tracking-tight text-slate-900">Set up your skill profile</h1>
        <p className="mt-2 text-slate-500">This takes about two minutes and powers your matches.</p>

        <ol className="mt-8 grid grid-cols-3 gap-2">
          {STEPS.map((s, i) => (
            <li key={s.title} className={`flex items-center gap-2 rounded-xl border px-3 py-2.5 text-sm font-medium ${
              i === step ? 'border-brand-300 bg-white text-brand-700 shadow-soft' : i < step ? 'border-emerald-200 bg-emerald-50 text-emerald-700' : 'border-slate-200 text-slate-400'
            }`}>
              <span className="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-current/10">
                {i < step ? <Check size={14} /> : <s.icon size={14} />}
              </span>
              <span className="hidden truncate sm:block">{s.title}</span>
            </li>
          ))}
        </ol>

        <div className="card mt-6 p-5 sm:p-8">
          {step === 0 && (
            <ProfileBasicsForm
              profile={profile}
              submitLabel="Continue"
              onSaved={(saved) => {
                setData(saved)
                setStep(1)
              }}
            />
          )}
          {step === 1 && (
            <>
              <h2 className="section-title">What can you teach?</h2>
              <p className="muted mb-5">Every hour you teach earns you one Skill Credit.</p>
              <SkillEditor type="TEACH" skills={profile.teachSkills} onChange={(teachSkills) => setData((p) => ({ ...p, teachSkills }))} />
            </>
          )}
          {step === 2 && (
            <>
              <h2 className="section-title">What do you want to learn?</h2>
              <p className="muted mb-5">We'll find students who teach these — ideally ones who want to learn what you teach.</p>
              <SkillEditor type="LEARN" skills={profile.learnSkills} onChange={(learnSkills) => setData((p) => ({ ...p, learnSkills }))} />
            </>
          )}
          {step > 0 && (
            <div className="mt-8 flex justify-between border-t border-slate-100 pt-5">
              <Button variant="ghost" icon={ArrowLeft} onClick={() => setStep(step - 1)}>
                Back
              </Button>
              {step < 2 ? (
                <Button onClick={() => setStep(step + 1)}>
                  Continue <ArrowRight size={16} />
                </Button>
              ) : (
                <Button variant="success" icon={Check} loading={finishing} onClick={finish}>
                  Finish setup
                </Button>
              )}
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
