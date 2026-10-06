import { useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import Button from '../components/ui/Button'
import ProfileBasicsForm from '../components/ProfileBasicsForm'
import SkillEditor from '../components/SkillEditor'
import { PageHeader } from '../components/ui/Layout'
import { CardSkeleton, ErrorState } from '../components/ui/Feedback'
import { useAsync } from '../hooks/useAsync'
import { userService } from '../services'
import { useToast } from '../context/ToastContext'

export default function EditProfile() {
  const navigate = useNavigate()
  const toast = useToast()
  const { data: profile, loading, error, reload, setData } = useAsync(() => userService.myProfile(), [])

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        title="Edit profile"
        subtitle="Keep your skills and availability up to date for better matches."
        actions={<Button variant="secondary" icon={ArrowLeft} to="/profile">Back to profile</Button>}
      />
      {loading ? (
        <CardSkeleton lines={6} />
      ) : error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : (
        <div className="space-y-6">
          <section className="card p-5 sm:p-6">
            <h2 className="section-title mb-5">Personal details</h2>
            <ProfileBasicsForm
              profile={profile}
              onSaved={(saved) => {
                setData(saved)
                toast.success('Profile saved')
                navigate('/profile')
              }}
            />
          </section>
          <section className="card p-5 sm:p-6">
            <h2 className="section-title">Skills I can teach</h2>
            <p className="muted mb-4">Changes are saved as you add or remove skills.</p>
            <SkillEditor type="TEACH" skills={profile.teachSkills} onChange={(teachSkills) => setData((p) => ({ ...p, teachSkills }))} />
          </section>
          <section className="card p-5 sm:p-6">
            <h2 className="section-title">Skills I want to learn</h2>
            <p className="muted mb-4">We use these to recommend teachers.</p>
            <SkillEditor type="LEARN" skills={profile.learnSkills} onChange={(learnSkills) => setData((p) => ({ ...p, learnSkills }))} />
          </section>
        </div>
      )}
    </div>
  )
}
