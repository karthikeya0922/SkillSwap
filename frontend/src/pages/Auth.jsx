import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { Eye, EyeOff, LogIn, UserPlus } from 'lucide-react'
import Logo from '../components/Logo'
import Button from '../components/ui/Button'
import { Field, Input, Select } from '../components/ui/Field'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

function AuthShell({ title, subtitle, children, footer }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="flex flex-col px-4 py-8 sm:px-10">
        <Logo />
        <div className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-10">
          <h1 className="text-3xl font-bold tracking-tight text-slate-900">{title}</h1>
          <p className="mt-2 text-slate-500">{subtitle}</p>
          <div className="mt-8">{children}</div>
          <p className="mt-6 text-center text-sm text-slate-500">{footer}</p>
        </div>
      </div>
      <div className="relative hidden overflow-hidden bg-gradient-to-br from-brand-600 via-brand-700 to-violet-700 lg:block">
        <div className="absolute -top-24 -right-24 h-96 w-96 rounded-full bg-white/10 blur-3xl" />
        <div className="absolute bottom-0 left-0 h-80 w-80 rounded-full bg-orange-300/20 blur-3xl" />
        <div className="relative flex h-full flex-col justify-end p-12 text-white">
          <p className="text-4xl leading-tight font-bold">“I taught Java for two hours and learned UI/UX in return. No money changed hands — just time.”</p>
          <p className="mt-6 text-brand-100">Karthikeya · Computer Science, Year 3</p>
          <div className="mt-10 grid grid-cols-3 gap-4 text-center">
            {[['1 hr', '= 1 credit'], ['92%', 'match score'], ['4.8★', 'avg rating']].map(([a, b]) => (
              <div key={b} className="rounded-2xl bg-white/10 p-4 backdrop-blur">
                <p className="text-2xl font-bold">{a}</p>
                <p className="text-xs text-brand-100">{b}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}

function PasswordInput({ error, ...props }) {
  const [show, setShow] = useState(false)
  return (
    <div className="relative">
      <Input type={show ? 'text' : 'password'} error={error} className="pr-11" {...props} />
      <button type="button" onClick={() => setShow((s) => !s)} className="absolute top-1/2 right-3 -translate-y-1/2 text-slate-400 hover:text-slate-600" aria-label={show ? 'Hide password' : 'Show password'}>
        {show ? <EyeOff size={17} /> : <Eye size={17} />}
      </button>
    </div>
  )
}

/** Maps server-side field errors onto react-hook-form fields. */
function applyServerErrors(error, setError) {
  if (error.fieldErrors) {
    Object.entries(error.fieldErrors).forEach(([field, message]) => setError(field, { message }))
  }
}

export function Login() {
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const [serverError, setServerError] = useState(null)
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm()

  const onSubmit = async (data) => {
    setServerError(null)
    try {
      const user = await login(data)
      toast.success(`Welcome back, ${user.fullName.split(' ')[0]}!`)
      const fallback = user.role === 'ADMIN' ? '/admin' : user.profileCompleted ? '/dashboard' : '/onboarding'
      navigate(location.state?.from && user.role !== 'ADMIN' ? location.state.from : fallback, { replace: true })
    } catch (e) {
      applyServerErrors(e, setError)
      setServerError(e.message)
    }
  }

  return (
    <AuthShell
      title="Welcome back"
      subtitle="Sign in to continue learning and teaching."
      footer={<>New to SkillSwap? <Link to="/register" className="link">Create an account</Link></>}
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        {serverError && <div className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{serverError}</div>}
        <Field label="College email" error={errors.email?.message}>
          <Input type="email" autoComplete="email" placeholder="you@college.edu" error={errors.email} {...register('email', { required: 'Email is required' })} />
        </Field>
        <Field label="Password" error={errors.password?.message}>
          <PasswordInput autoComplete="current-password" placeholder="••••••••" error={errors.password} {...register('password', { required: 'Password is required' })} />
        </Field>
        <Button type="submit" size="lg" className="w-full" icon={LogIn} loading={isSubmitting}>
          Sign in
        </Button>
      </form>
    </AuthShell>
  )
}

export function Register() {
  const { register: signUp } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState(null)
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm({ defaultValues: { yearOfStudy: '' } })

  const onSubmit = async (data) => {
    setServerError(null)
    try {
      await signUp({ ...data, email: data.email.trim(), yearOfStudy: Number(data.yearOfStudy) })
      toast.success('Account created!', "Let's set up your skill profile.")
      navigate('/onboarding', { replace: true })
    } catch (e) {
      applyServerErrors(e, setError)
      setServerError(e.message)
    }
  }

  return (
    <AuthShell
      title="Create your account"
      subtitle="Join your campus skill exchange. You'll get 5 starter credits."
      footer={<>Already have an account? <Link to="/login" className="link">Sign in</Link></>}
    >
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        {serverError && <div className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{serverError}</div>}
        <Field label="Full name" error={errors.fullName?.message}>
          <Input autoComplete="name" placeholder="Karthikeya Gupta" error={errors.fullName} {...register('fullName', { required: 'Full name is required', minLength: { value: 2, message: 'Name is too short' } })} />
        </Field>
        <Field label="Email" error={errors.email?.message}>
          <Input type="email" autoComplete="email" placeholder="you@college.edu" error={errors.email} {...register('email', { required: 'Email is required', pattern: { value: /^\S+@\S+\.\S+$/, message: 'Enter a valid email address' } })} />
        </Field>
        <Field label="Password" error={errors.password?.message} hint="At least 8 characters with a letter and a number.">
          <PasswordInput
            autoComplete="new-password"
            placeholder="Create a password"
            error={errors.password}
            {...register('password', {
              required: 'Password is required',
              minLength: { value: 8, message: 'Use at least 8 characters' },
              validate: (v) => (/[A-Za-z]/.test(v) && /\d/.test(v)) || 'Include at least one letter and one number',
            })}
          />
        </Field>
        <Field label="College / University" error={errors.college?.message}>
          <Input placeholder="CBIT Hyderabad" error={errors.college} {...register('college', { required: 'College is required' })} />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Department" error={errors.department?.message}>
            <Input placeholder="Computer Science" error={errors.department} {...register('department', { required: 'Department is required' })} />
          </Field>
          <Field label="Year of study" error={errors.yearOfStudy?.message}>
            <Select error={errors.yearOfStudy} {...register('yearOfStudy', { required: 'Choose your year' })}>
              <option value="">Select</option>
              {[1, 2, 3, 4, 5, 6].map((y) => (
                <option key={y} value={y}>Year {y}</option>
              ))}
            </Select>
          </Field>
        </div>
        <Button type="submit" size="lg" className="w-full" icon={UserPlus} loading={isSubmitting}>
          Create account
        </Button>
      </form>
    </AuthShell>
  )
}
