import React, { useState } from 'react'
import {
  BookOpen,
  Users,
  Award,
  Sparkles,
  Clock,
  Coins,
  MessageSquare,
  Search,
  CheckCircle2,
  TrendingUp,
  Compass,
  Star,
  ArrowRight,
  Filter,
  Calendar,
  ShieldCheck,
  Zap,
  ChevronRight,
  Bell,
  UserCheck
} from 'lucide-react'

const SAMPLE_SKILLS = [
  {
    id: 1,
    title: 'Full-Stack Spring Boot 4 & JPA',
    category: 'Software Engineering',
    instructor: 'Priya Raman',
    dept: 'Computer Science, 4th Year',
    rating: 4.9,
    reviews: 28,
    costCredits: 1,
    level: 'Advanced',
    tags: ['Spring Boot', 'Java 21', 'REST APIs', 'PostgreSQL'],
    bio: 'Built production microservices and campus portal. Happy to teach JPA internals & security.'
  },
  {
    id: 2,
    title: 'Modern React 19 & Tailwind UI',
    category: 'Frontend & Design',
    instructor: 'Karthik Rao',
    dept: 'Information Technology, 3rd Year',
    rating: 5.0,
    reviews: 34,
    costCredits: 1,
    level: 'Intermediate',
    tags: ['React', 'Tailwind', 'Vite', 'State Management'],
    bio: 'Frontend enthusiast. Specializes in building clean interactive SPAs and fluid micro-interactions.'
  },
  {
    id: 3,
    title: 'Algorithms, LeetCode & Competitive Coding',
    category: 'Computer Science',
    instructor: 'Aditya Sen',
    dept: 'Data Science, 3rd Year',
    rating: 4.8,
    reviews: 19,
    costCredits: 1,
    level: 'Advanced',
    tags: ['Graph Theory', 'DP', 'Trees', 'System Design'],
    bio: 'Knight on LeetCode (2100+). Mentored 40+ juniors for upcoming campus placements.'
  },
  {
    id: 4,
    title: 'Acoustic Guitar & Fingerpicking Basics',
    category: 'Music & Arts',
    instructor: 'Sneha Roy',
    dept: 'Electronics, 2nd Year',
    rating: 4.95,
    reviews: 15,
    costCredits: 1,
    level: 'Beginner',
    tags: ['Chords', 'Rhythm', 'Fingerstyle', 'Ear Training'],
    bio: 'Lead guitarist of the campus band. Teaching chord progressions and easy songs in 4 sessions.'
  },
  {
    id: 5,
    title: 'UI/UX Design Systems & Figma Prototyping',
    category: 'Frontend & Design',
    instructor: 'Tanvi Joshi',
    dept: 'Design & Media, 3rd Year',
    rating: 4.88,
    reviews: 22,
    costCredits: 1,
    level: 'All Levels',
    tags: ['Figma', 'Auto-Layout', 'Design Systems', 'UX Research'],
    bio: 'Ex-intern @ DesignCo. Focus on real-world wireframes, responsive grids, and design tokens.'
  },
  {
    id: 6,
    title: 'Spoken German for Academic Exchange (A1-A2)',
    category: 'Languages',
    instructor: 'Kabir Patel',
    dept: 'Mechanical Eng, 4th Year',
    rating: 4.92,
    reviews: 12,
    costCredits: 1,
    level: 'Beginner',
    tags: ['German', 'Pronunciation', 'Exchange Prep', 'Grammar'],
    bio: 'DAAD Scholar exchange student. Daily conversational practice and pronunciation training.'
  }
]

const SAMPLE_TRANSACTIONS = [
  { id: 'TX-104', title: 'Session Completed: Spring Boot REST Fundamentals', change: '+1 TC', type: 'earn', date: 'Today, 10:15 AM' },
  { id: 'TX-103', title: 'Booked: Figma Auto-layout masterclass', change: '-1 TC', type: 'spend', date: 'Yesterday' },
  { id: 'TX-102', title: 'Peer Review Reward (5-Star feedback given)', change: '+0.5 TC', type: 'earn', date: 'Oct 04' },
  { id: 'TX-101', title: 'SkillSwap Welcome Bonus', change: '+5.0 TC', type: 'bonus', date: 'Oct 01' },
]

export default function App() {
  const [activeTab, setActiveTab] = useState('browse')
  const [selectedCategory, setSelectedCategory] = useState('All')
  const [searchQuery, setSearchQuery] = useState('')
  const [credits, setCredits] = useState(5.0)
  const [bookedSessions, setBookedSessions] = useState([
    { id: 101, title: 'Figma Auto-layout masterclass', mentor: 'Tanvi Joshi', time: 'Tomorrow at 4:00 PM', status: 'Confirmed' }
  ])
  const [bookedSuccessMsg, setBookedSuccessMsg] = useState('')

  const categories = ['All', 'Software Engineering', 'Frontend & Design', 'Computer Science', 'Music & Arts', 'Languages']

  const filteredSkills = SAMPLE_SKILLS.filter(skill => {
    const matchesCategory = selectedCategory === 'All' || skill.category === selectedCategory
    const matchesSearch = skill.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          skill.instructor.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          skill.tags.some(tag => tag.toLowerCase().includes(searchQuery.toLowerCase()))
    return matchesCategory && matchesSearch
  })

  const handleBookSession = (skill) => {
    if (credits < skill.costCredits) {
      alert('Insufficient Time Credits! Teach a peer to earn credits or complete a community challenge.')
      return
    }
    setCredits(prev => Number((prev - skill.costCredits).toFixed(1)))
    const newSession = {
      id: Date.now(),
      title: skill.title,
      mentor: skill.instructor,
      time: 'Oct 08, 5:30 PM (Pending Confirmation)',
      status: 'Booked'
    }
    setBookedSessions(prev => [newSession, ...prev])
    setBookedSuccessMsg(`Successfully booked 1-hour session for "${skill.title}"! 1 Time Credit deducted.`)
    setTimeout(() => setBookedSuccessMsg(''), 4500)
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col font-sans">
      {/* Top Notification Bar */}
      {bookedSuccessMsg && (
        <div className="bg-emerald-500/20 border-b border-emerald-500/40 text-emerald-300 px-4 py-2 text-center text-sm font-medium flex items-center justify-center gap-2 transition-all">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>{bookedSuccessMsg}</span>
        </div>
      )}

      {/* Navigation Header */}
      <header className="sticky top-0 z-50 glass-panel border-b border-slate-800/80 bg-slate-950/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center shadow-lg shadow-indigo-500/30">
              <Sparkles className="w-5 h-5 text-white" />
            </div>
            <div>
              <span className="text-xl font-bold tracking-tight bg-gradient-to-r from-white via-indigo-100 to-indigo-300 bg-clip-text text-transparent">
                SkillSwap
              </span>
              <span className="hidden sm:inline-block ml-2 text-xs font-semibold px-2 py-0.5 rounded-full bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                Campus P2P
              </span>
            </div>
          </div>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center gap-1">
            <button
              onClick={() => setActiveTab('browse')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                activeTab === 'browse' ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Discover Skills
            </button>
            <button
              onClick={() => setActiveTab('matching')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                activeTab === 'matching' ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Match Engine
            </button>
            <button
              onClick={() => setActiveTab('sessions')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                activeTab === 'sessions' ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              My Sessions ({bookedSessions.length})
            </button>
            <button
              onClick={() => setActiveTab('wallet')}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                activeTab === 'wallet' ? 'bg-indigo-600/20 text-indigo-300 border border-indigo-500/30' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Time Wallet
            </button>
          </nav>

          {/* User Status / Time Credit Pill */}
          <div className="flex items-center gap-3">
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-slate-900 border border-slate-800">
              <Coins className="w-4 h-4 text-amber-400" />
              <span className="text-xs font-semibold text-slate-300">
                <strong className="text-amber-300 font-bold">{credits.toFixed(1)}</strong> TC
              </span>
            </div>

            <div className="flex items-center gap-2 pl-2 border-l border-slate-800">
              <div className="w-8 h-8 rounded-full bg-gradient-to-br from-violet-600 to-indigo-700 flex items-center justify-center text-xs font-bold ring-2 ring-indigo-500/30">
                AS
              </div>
              <div className="hidden lg:block text-left text-xs">
                <div className="font-semibold text-slate-200 leading-tight">Aarav Sharma</div>
                <div className="text-slate-400 text-[10px]">1,240 XP · Level 4</div>
              </div>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
        {/* Banner */}
        <section className="mb-10 text-center relative overflow-hidden rounded-3xl p-8 sm:p-12 border border-slate-800 bg-gradient-to-b from-indigo-950/40 via-slate-900/50 to-slate-950 accent-glow">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/30 text-indigo-400 text-xs font-medium mb-4">
            <Zap className="w-3.5 h-3.5 text-indigo-400" />
            <span>Fair 1:1 Peer Knowledge Exchange · No Currency Required</span>
          </div>
          <h1 className="text-3xl sm:text-5xl font-extrabold tracking-tight text-white mb-4">
            Trade What You Know. <br />
            <span className="bg-gradient-to-r from-indigo-400 via-violet-300 to-teal-300 bg-clip-text text-transparent">
              Learn What You Want.
            </span>
          </h1>
          <p className="max-w-2xl mx-auto text-slate-400 text-sm sm:text-base mb-8">
            Connect with peers across campus for verified 1-on-1 tutoring sessions. Earn 1 Time Credit for every hour you teach, and spend it learning anything from algorithms to guitar.
          </p>

          {/* Quick Metrics */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 max-w-3xl mx-auto">
            <div className="glass-card p-4 rounded-xl text-center">
              <div className="text-xl sm:text-2xl font-bold text-indigo-300">50+</div>
              <div className="text-xs text-slate-400 mt-0.5">Campus Skills</div>
            </div>
            <div className="glass-card p-4 rounded-xl text-center">
              <div className="text-xl sm:text-2xl font-bold text-violet-300">100%</div>
              <div className="text-xs text-slate-400 mt-0.5">Time Banked (1:1)</div>
            </div>
            <div className="glass-card p-4 rounded-xl text-center">
              <div className="text-xl sm:text-2xl font-bold text-emerald-300">4.9 ★</div>
              <div className="text-xs text-slate-400 mt-0.5">Peer Verified Ratings</div>
            </div>
            <div className="glass-card p-4 rounded-xl text-center">
              <div className="text-xl sm:text-2xl font-bold text-amber-300">Live</div>
              <div className="text-xs text-slate-400 mt-0.5">STOMP Chat & Presence</div>
            </div>
          </div>
        </section>

        {/* Tab View: Browse Skills */}
        {activeTab === 'browse' && (
          <div>
            {/* Search and Filters */}
            <div className="flex flex-col md:flex-row gap-4 items-center justify-between mb-8">
              <div className="relative w-full md:w-96">
                <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Search skills, topics, or mentors..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-xl pl-10 pr-4 py-2 text-sm text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all"
                />
              </div>

              {/* Categories */}
              <div className="flex items-center gap-2 overflow-x-auto w-full md:w-auto pb-2 md:pb-0">
                {categories.map(cat => (
                  <button
                    key={cat}
                    onClick={() => setSelectedCategory(cat)}
                    className={`whitespace-nowrap px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                      selectedCategory === cat
                        ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                        : 'bg-slate-900 text-slate-400 hover:text-slate-200 hover:bg-slate-800 border border-slate-800'
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>
            </div>

            {/* Skills Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredSkills.map(skill => (
                <div key={skill.id} className="glass-card rounded-2xl p-6 flex flex-col justify-between">
                  <div>
                    <div className="flex items-start justify-between gap-3 mb-3">
                      <span className="text-[11px] font-semibold uppercase tracking-wider text-indigo-400 bg-indigo-500/10 px-2.5 py-1 rounded-md border border-indigo-500/20">
                        {skill.category}
                      </span>
                      <div className="flex items-center gap-1 text-amber-400 text-xs font-semibold">
                        <Star className="w-3.5 h-3.5 fill-amber-400" />
                        <span>{skill.rating}</span>
                        <span className="text-slate-500 font-normal">({skill.reviews})</span>
                      </div>
                    </div>

                    <h3 className="text-lg font-bold text-white mb-2 leading-snug">
                      {skill.title}
                    </h3>
                    <p className="text-xs text-slate-400 mb-4 line-clamp-2">
                      {skill.bio}
                    </p>

                    <div className="flex flex-wrap gap-1.5 mb-5">
                      {skill.tags.map(tag => (
                        <span key={tag} className="text-[11px] bg-slate-800/80 text-slate-300 px-2 py-0.5 rounded-md border border-slate-700/50">
                          {tag}
                        </span>
                      ))}
                    </div>
                  </div>

                  <div className="pt-4 border-t border-slate-800/80">
                    <div className="flex items-center justify-between mb-4">
                      <div className="flex items-center gap-2">
                        <div className="w-7 h-7 rounded-full bg-slate-800 flex items-center justify-center text-xs font-bold text-slate-300">
                          {skill.instructor.charAt(0)}
                        </div>
                        <div>
                          <div className="text-xs font-semibold text-slate-200">{skill.instructor}</div>
                          <div className="text-[10px] text-slate-500">{skill.dept}</div>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-xs font-bold text-amber-300">{skill.costCredits} TC / hr</div>
                        <div className="text-[10px] text-slate-500">{skill.level}</div>
                      </div>
                    </div>

                    <button
                      onClick={() => handleBookSession(skill)}
                      className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-medium py-2 px-4 rounded-xl text-xs flex items-center justify-center gap-1.5 transition-all shadow-md shadow-indigo-600/20 active:scale-[0.98]"
                    >
                      <Calendar className="w-3.5 h-3.5" />
                      <span>Book 1-on-1 Session</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tab View: Matching Engine */}
        {activeTab === 'matching' && (
          <div className="glass-card rounded-2xl p-6 sm:p-8 max-w-4xl mx-auto">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-xl bg-violet-600/20 text-violet-400 flex items-center justify-center border border-violet-500/30">
                <Compass className="w-5 h-5" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-white">Deterministic Peer Matching Engine</h2>
                <p className="text-xs text-slate-400">Calculates mutual skill affinity, schedule overlap, and skill proficiency reciprocity.</p>
              </div>
            </div>

            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-slate-900/90 border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center font-bold text-white text-sm">
                    PR
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-white text-sm">Priya Raman</span>
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                        98% Match Score
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-1">
                      Teaches: <strong className="text-indigo-300">Spring Boot 4</strong> ↔ Wants to learn: <strong className="text-teal-300">React 19 & Tailwind</strong>
                    </p>
                  </div>
                </div>

                <button
                  onClick={() => alert('Mutual match connection request sent to Priya Raman!')}
                  className="w-full sm:w-auto bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all"
                >
                  <UserCheck className="w-3.5 h-3.5" />
                  <span>Send Swap Request</span>
                </button>
              </div>

              <div className="p-4 rounded-xl bg-slate-900/90 border border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-full bg-gradient-to-tr from-amber-500 to-rose-600 flex items-center justify-center font-bold text-white text-sm">
                    AS
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-white text-sm">Aditya Sen</span>
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-indigo-500/20 text-indigo-400 border border-indigo-500/30">
                        92% Match Score
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-1">
                      Teaches: <strong className="text-indigo-300">Dynamic Programming</strong> ↔ Wants to learn: <strong className="text-teal-300">UI Prototyping</strong>
                    </p>
                  </div>
                </div>

                <button
                  onClick={() => alert('Mutual match connection request sent to Aditya Sen!')}
                  className="w-full sm:w-auto bg-indigo-600 hover:bg-indigo-500 text-white px-4 py-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-all"
                >
                  <UserCheck className="w-3.5 h-3.5" />
                  <span>Send Swap Request</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Tab View: My Sessions */}
        {activeTab === 'sessions' && (
          <div className="glass-card rounded-2xl p-6 sm:p-8 max-w-4xl mx-auto">
            <h2 className="text-xl font-bold text-white mb-2">Booked & Completed Learning Sessions</h2>
            <p className="text-xs text-slate-400 mb-6">Each 1-hour session automatically locks 1 Time Credit in escrow until both peers mark the session complete.</p>

            <div className="space-y-3">
              {bookedSessions.map((s) => (
                <div key={s.id} className="p-4 rounded-xl bg-slate-900 border border-slate-800 flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-lg bg-indigo-500/10 text-indigo-400 flex items-center justify-center">
                      <BookOpen className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-white">{s.title}</div>
                      <div className="text-xs text-slate-400">Mentor: {s.mentor} · {s.time}</div>
                    </div>
                  </div>
                  <span className="text-xs font-semibold px-2.5 py-1 rounded-md bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                    {s.status}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tab View: Time Wallet Ledger */}
        {activeTab === 'wallet' && (
          <div className="glass-card rounded-2xl p-6 sm:p-8 max-w-4xl mx-auto">
            <div className="flex items-center justify-between mb-6 pb-6 border-b border-slate-800">
              <div>
                <h2 className="text-xl font-bold text-white">Time-Credit Wallet Ledger</h2>
                <p className="text-xs text-slate-400">Immutable credit audit log. 1 TC = 1 Hour of peer teaching.</p>
              </div>
              <div className="text-right">
                <div className="text-2xl font-black text-amber-300">{credits.toFixed(1)} TC</div>
                <div className="text-[11px] text-slate-500">Available Balance</div>
              </div>
            </div>

            <div className="space-y-3">
              {SAMPLE_TRANSACTIONS.map(tx => (
                <div key={tx.id} className="p-3.5 rounded-xl bg-slate-900 border border-slate-800/80 flex items-center justify-between text-xs">
                  <div>
                    <div className="font-medium text-slate-200">{tx.title}</div>
                    <div className="text-[10px] text-slate-500">{tx.id} · {tx.date}</div>
                  </div>
                  <span className={`font-bold font-mono ${tx.change.startsWith('+') ? 'text-emerald-400' : 'text-rose-400'}`}>
                    {tx.change}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="glass-panel border-t border-slate-800/80 mt-16 py-8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-indigo-400" />
            <span>SkillSwap Platform · Campus Peer-to-Peer Learning</span>
          </div>
          <div>
            Spring Boot 4 Backend & Vite React Frontend · Time-Credit Protocol
          </div>
        </div>
      </footer>
    </div>
  )
}
