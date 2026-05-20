// web-screens.jsx — Web giver flow, rendered inside browser windows

// ─────────────────────────────────────────────────────────────
// Shared atoms
// ─────────────────────────────────────────────────────────────

function Logo({ t, size = 22, withTag = false }) {
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
      <span style={{
        fontFamily: TYPE.display, fontSize: size, fontStyle: 'italic',
        color: t.ink, letterSpacing: -0.2, lineHeight: 1,
      }}>giftmaison<span style={{ color: t.accent }}>.</span></span>
      {withTag && (
        <span style={{
          fontFamily: TYPE.mono, fontSize: 10, color: t.inkFaint,
          textTransform: 'uppercase', letterSpacing: 1.5,
        }}>gift registry</span>
      )}
    </div>
  );
}

function Pill({ t, children, tone = 'neutral', size = 'sm' }) {
  const tones = {
    neutral: { bg: t.paperDeep, fg: t.inkSoft, border: t.line },
    accent: { bg: t.accentSoft, fg: t.accent, border: 'transparent' },
    second: { bg: t.secondSoft, fg: t.second, border: 'transparent' },
    ok: { bg: 'oklch(0.94 0.04 150)', fg: t.ok, border: 'transparent' },
    warn: { bg: 'oklch(0.95 0.04 70)', fg: t.warn, border: 'transparent' },
  };
  const tn = tones[tone];
  const pad = size === 'sm' ? '3px 8px' : '5px 10px';
  const fs = size === 'sm' ? 11 : 12;
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 5,
      padding: pad, borderRadius: 999, background: tn.bg, color: tn.fg,
      border: `1px solid ${tn.border}`, fontFamily: TYPE.mono, fontSize: fs,
      letterSpacing: 0.3, textTransform: 'uppercase', fontWeight: 500,
      whiteSpace: 'nowrap',
    }}>{children}</span>
  );
}

function Btn({ t, children, variant = 'primary', size = 'md', icon, style = {}, ...rest }) {
  const base = {
    primary: { bg: t.ink, fg: t.paper, border: t.ink },
    accent: { bg: t.accent, fg: t.accentInk, border: t.accent },
    ghost: { bg: 'transparent', fg: t.ink, border: t.line },
    quiet: { bg: 'transparent', fg: t.inkSoft, border: 'transparent' },
  };
  const v = base[variant];
  const pad = size === 'lg' ? '14px 22px' : size === 'sm' ? '7px 12px' : '11px 18px';
  const fs = size === 'lg' ? 15 : size === 'sm' ? 12 : 13.5;
  return (
    <button style={{
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: 8,
      padding: pad, borderRadius: 999, background: v.bg, color: v.fg,
      border: `1px solid ${v.border}`, fontFamily: TYPE.body, fontSize: fs,
      fontWeight: 500, letterSpacing: -0.1, cursor: 'pointer', lineHeight: 1,
      ...style,
    }} {...rest}>
      {icon}{children}
    </button>
  );
}

function Field({ t, label, value, placeholder, hint, autofilled, prefix, suffix }) {
  return (
    <label style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
      <span style={{
        fontFamily: TYPE.mono, fontSize: 10, textTransform: 'uppercase',
        letterSpacing: 1.3, color: t.inkFaint, fontWeight: 500,
      }}>{label}{autofilled && <span style={{ color: t.ok, marginLeft: 8 }}>✓ auto-filled</span>}</span>
      <div style={{
        display: 'flex', alignItems: 'center', gap: 8,
        padding: '11px 14px', borderRadius: 8,
        background: t.paper, border: `1px solid ${t.line}`,
        fontFamily: TYPE.body, fontSize: 14, color: value ? t.ink : t.inkFaint,
      }}>
        {prefix && <span style={{ color: t.inkFaint, fontSize: 13 }}>{prefix}</span>}
        <span style={{ flex: 1 }}>{value || placeholder}</span>
        {suffix}
      </div>
      {hint && <span style={{ fontSize: 11.5, color: t.inkFaint, fontFamily: TYPE.body }}>{hint}</span>}
    </label>
  );
}

// ─────────────────────────────────────────────────────────────
// Web: top nav (shared by all web artboards)
// ─────────────────────────────────────────────────────────────
function WebNav({ t, signedIn = false }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '20px 40px', borderBottom: `1px solid ${t.line}`,
      background: t.paper,
    }}>
      <Logo t={t} size={24} withTag />
      <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
        <span style={{ fontFamily: TYPE.mono, fontSize: 11, color: t.inkFaint, letterSpacing: 0.5 }}>EN / RO</span>
        {signedIn ? (
          <div style={{
            width: 32, height: 32, borderRadius: '50%', background: t.second,
            color: t.paper, display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontFamily: TYPE.body, fontSize: 12, fontWeight: 500,
          }}>AM</div>
        ) : (
          <Btn t={t} variant="ghost" size="sm">Sign in</Btn>
        )}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// Item card (web grid)
// ─────────────────────────────────────────────────────────────
function ItemCard({ t, item, featured = false }) {
  const isReserved = item.status === 'reserved';
  const isPurchased = item.status === 'purchased';
  const isAvailable = item.status === 'available';

  return (
    <div style={{
      background: t.paper, borderRadius: 14, overflow: 'hidden',
      border: `1px solid ${t.line}`,
      display: 'flex', flexDirection: 'column',
      opacity: isPurchased ? 0.55 : 1,
    }}>
      <div style={{
        position: 'relative', aspectRatio: featured ? '16 / 10' : '4 / 3',
        background: t.paperDeep, overflow: 'hidden',
      }}>
        <img src={item.image} alt="" style={{
          width: '100%', height: '100%', objectFit: 'cover',
          filter: isPurchased ? 'grayscale(1)' : 'none',
        }} />
        {isPurchased && (
          <div style={{
            position: 'absolute', inset: 0, display: 'flex', alignItems: 'center',
            justifyContent: 'center',
          }}>
            <div style={{
              background: t.paper, padding: '6px 14px', borderRadius: 999,
              fontFamily: TYPE.mono, fontSize: 11, color: t.inkSoft,
              textTransform: 'uppercase', letterSpacing: 1.3, fontWeight: 500,
            }}>✓ Given by {item.purchasedBy}</div>
          </div>
        )}
        <div style={{ position: 'absolute', top: 12, left: 12 }}>
          {isAvailable && <Pill t={t} tone="neutral">Available</Pill>}
          {isReserved && <Pill t={t} tone="accent">◉ Reserved</Pill>}
          {isPurchased && <Pill t={t} tone="ok">Purchased</Pill>}
        </div>
      </div>
      <div style={{ padding: '14px 16px 16px', display: 'flex', flexDirection: 'column', gap: 12, flex: 1 }}>
        <div>
          <h3 style={{
            margin: 0, fontFamily: TYPE.body, fontSize: featured ? 17 : 15,
            fontWeight: 500, color: t.ink, lineHeight: 1.25, letterSpacing: -0.2,
          }}>{item.title}</h3>
          <div style={{
            display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
            marginTop: 6,
          }}>
            <span style={{ fontFamily: TYPE.body, fontSize: 14, color: t.ink, fontWeight: 500 }}>
              {item.price} <span style={{ color: t.inkFaint, fontSize: 11, fontFamily: TYPE.mono }}>{item.currency}</span>
            </span>
            <span style={{ fontFamily: TYPE.mono, fontSize: 10, color: t.inkFaint, letterSpacing: 0.5 }}>
              {item.retailer}
            </span>
          </div>
        </div>

        {isReserved && (
          <div style={{
            display: 'flex', alignItems: 'center', gap: 10, padding: '9px 12px',
            background: t.accentSoft, borderRadius: 8,
          }}>
            <div style={{
              width: 8, height: 8, borderRadius: '50%', background: t.accent,
              boxShadow: `0 0 0 4px ${t.accentSoft}`,
              animation: 'pulse 1.6s ease-in-out infinite',
            }} />
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 1 }}>
              <span style={{ fontFamily: TYPE.body, fontSize: 12, color: t.accent, fontWeight: 500 }}>
                Reserved by {item.reservedBy}
              </span>
              <span style={{ fontFamily: TYPE.mono, fontSize: 10.5, color: t.accent, letterSpacing: 0.4 }}>
                {item.minutesLeft} MIN LEFT · auto-releases if not purchased
              </span>
            </div>
          </div>
        )}

        {isAvailable && (
          <Btn t={t} variant="primary" size="sm" style={{ alignSelf: 'stretch' }}>
            Reserve this gift →
          </Btn>
        )}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// SCREEN 1: Registry detail (hero)
// ─────────────────────────────────────────────────────────────
function RegistryScreen({ t }) {
  return (
    <div style={{ background: t.paper, minHeight: '100%', display: 'flex', flexDirection: 'column' }}>
      <WebNav t={t} />

      {/* Registry header */}
      <div style={{ padding: '48px 40px 36px', borderBottom: `1px solid ${t.line}` }}>
        <div style={{ display: 'flex', gap: 40, alignItems: 'flex-end', justifyContent: 'space-between' }}>
          <div style={{ flex: 1, maxWidth: 640 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 18 }}>
              <Pill t={t} tone="accent">⌂ Housewarming</Pill>
              <Pill t={t} tone="neutral">Public link</Pill>
              <span style={{ fontFamily: TYPE.mono, fontSize: 11, color: t.inkFaint, letterSpacing: 0.4 }}>
                · May 16, 2026
              </span>
            </div>
            <h1 style={{
              margin: 0, fontFamily: TYPE.display, fontSize: 58, fontWeight: 400,
              color: t.ink, lineHeight: 0.96, letterSpacing: -1.5,
            }}>
              Ana & Mihai, <span style={{ fontStyle: 'italic', color: t.accent }}>at last</span> on Strada Popa
            </h1>
            <p style={{
              margin: '18px 0 0', fontFamily: TYPE.body, fontSize: 16,
              color: t.inkSoft, lineHeight: 1.55, maxWidth: 540, textWrap: 'pretty',
            }}>
              Seven years, three moves, one very patient cat — and finally keys to a place we own.
              If you'd like to mark the occasion, we've put together a little list. No pressure, no
              duplicates, no re-gifting the teapot from the wedding.
            </p>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12, alignItems: 'flex-end' }}>
            <div style={{
              padding: '14px 18px', background: t.paperDeep, borderRadius: 10,
              display: 'flex', flexDirection: 'column', gap: 8, minWidth: 200,
            }}>
              <span style={{ fontFamily: TYPE.mono, fontSize: 10, color: t.inkFaint, letterSpacing: 1.2, textTransform: 'uppercase' }}>
                Progress
              </span>
              <div style={{ display: 'flex', alignItems: 'baseline', gap: 6 }}>
                <span style={{ fontFamily: TYPE.display, fontSize: 34, color: t.ink, lineHeight: 1 }}>2</span>
                <span style={{ fontFamily: TYPE.body, fontSize: 14, color: t.inkFaint }}>of 12 chosen</span>
              </div>
              <div style={{ height: 4, background: t.line, borderRadius: 2, overflow: 'hidden' }}>
                <div style={{ width: '16.6%', height: '100%', background: t.accent }} />
              </div>
            </div>
            <Btn t={t} variant="ghost" size="sm" icon="↗">Share this registry</Btn>
          </div>
        </div>
      </div>

      {/* Item grid */}
      <div style={{ padding: '32px 40px 48px' }}>
        <div style={{
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          marginBottom: 20,
        }}>
          <h2 style={{
            margin: 0, fontFamily: TYPE.display, fontSize: 24, fontWeight: 400,
            color: t.ink, letterSpacing: -0.5,
          }}>The list <span style={{ color: t.inkFaint, fontFamily: TYPE.mono, fontSize: 13, letterSpacing: 0.5 }}>— 12 items</span></h2>
          <div style={{ display: 'flex', gap: 6, padding: 4, background: t.paperDeep, borderRadius: 999 }}>
            {['All', 'Available', 'Reserved', 'Purchased'].map((x, i) => (
              <button key={x} style={{
                padding: '6px 14px', borderRadius: 999, border: 'none',
                background: i === 0 ? t.paper : 'transparent',
                boxShadow: i === 0 ? '0 1px 2px rgba(0,0,0,0.06)' : 'none',
                fontFamily: TYPE.body, fontSize: 12.5, fontWeight: 500,
                color: i === 0 ? t.ink : t.inkFaint, cursor: 'pointer',
              }}>{x}</button>
            ))}
          </div>
        </div>

        <div style={{
          display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20,
        }}>
          {SAMPLE_ITEMS.slice(0, 9).map(it => <ItemCard key={it.id} t={t} item={it} />)}
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// SCREEN 2: Reserve flow — just-reserved state with timer
// ─────────────────────────────────────────────────────────────
function ReserveScreen({ t }) {
  const reserved = { ...SAMPLE_ITEMS[1], status: 'reserved', reservedBy: 'you', minutesLeft: 29 };
  return (
    <div style={{ background: t.paper, minHeight: '100%' }}>
      <WebNav t={t} signedIn />

      {/* Sticky reservation banner */}
      <div style={{
        background: t.ink, color: t.paper, padding: '14px 40px',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 20,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <div style={{
            width: 10, height: 10, borderRadius: '50%', background: t.accent,
            boxShadow: `0 0 0 5px ${t.accent}30`,
          }} />
          <div>
            <div style={{ fontFamily: TYPE.body, fontSize: 14, fontWeight: 500 }}>
              You reserved <em style={{ fontFamily: TYPE.display, fontStyle: 'italic', fontWeight: 400 }}>Sculptural table lamp</em>
            </div>
            <div style={{ fontFamily: TYPE.mono, fontSize: 11, opacity: 0.65, letterSpacing: 0.5, marginTop: 2 }}>
              29:47 remaining · finish your purchase at dedeman.ro
            </div>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <Btn t={t} variant="quiet" size="sm" style={{ color: t.paper, border: `1px solid ${t.paper}30` }}>
            Release reservation
          </Btn>
          <Btn t={t} variant="accent" size="sm">
            Continue to dedeman.ro →
          </Btn>
        </div>
      </div>

      <div style={{ padding: '36px 40px' }}>
        <div style={{
          display: 'grid', gridTemplateColumns: '1fr 340px', gap: 32, alignItems: 'flex-start',
        }}>
          {/* Main: the reserved item, detailed */}
          <div>
            <span style={{ fontFamily: TYPE.mono, fontSize: 11, color: t.inkFaint, letterSpacing: 1.2, textTransform: 'uppercase' }}>
              Your reservation · Step 2 of 2
            </span>
            <h1 style={{
              margin: '10px 0 28px', fontFamily: TYPE.display, fontSize: 44, fontWeight: 400,
              color: t.ink, letterSpacing: -1, lineHeight: 1.05,
            }}>
              Nice one. <span style={{ fontStyle: 'italic', color: t.accent }}>Now finish the purchase</span> at the retailer.
            </h1>

            <div style={{
              display: 'flex', gap: 20, padding: 20,
              background: t.paperDeep, borderRadius: 14, border: `1px solid ${t.line}`,
            }}>
              <div style={{
                width: 160, height: 160, borderRadius: 10, overflow: 'hidden', flexShrink: 0,
                background: t.line,
              }}>
                <img src={reserved.image} style={{ width: '100%', height: '100%', objectFit: 'cover' }} alt="" />
              </div>
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 14, justifyContent: 'space-between' }}>
                <div>
                  <Pill t={t} tone="accent">◉ Reserved for you</Pill>
                  <h2 style={{
                    margin: '10px 0 4px', fontFamily: TYPE.body, fontSize: 20,
                    fontWeight: 500, color: t.ink, letterSpacing: -0.3,
                  }}>{reserved.title}</h2>
                  <div style={{ fontFamily: TYPE.body, fontSize: 14, color: t.inkSoft }}>
                    {reserved.price} <span style={{ color: t.inkFaint }}>{reserved.currency}</span>
                    <span style={{ color: t.inkFaint, margin: '0 8px' }}>·</span>
                    sold at <strong style={{ fontWeight: 500, color: t.ink }}>{reserved.retailer}</strong>
                  </div>
                </div>
                <div style={{
                  padding: '12px 14px', background: t.paper, borderRadius: 8,
                  border: `1px solid ${t.line}`,
                }}>
                  <div style={{
                    display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
                    marginBottom: 8,
                  }}>
                    <span style={{ fontFamily: TYPE.mono, fontSize: 10, color: t.inkFaint, letterSpacing: 1.2, textTransform: 'uppercase' }}>
                      Time to purchase
                    </span>
                    <span style={{ fontFamily: TYPE.mono, fontSize: 12, color: t.accent, fontWeight: 500 }}>
                      29:47
                    </span>
                  </div>
                  <div style={{ height: 3, background: t.line, borderRadius: 2, overflow: 'hidden' }}>
                    <div style={{ width: '99%', height: '100%', background: t.accent, transition: 'width 1s linear' }} />
                  </div>
                </div>
              </div>
            </div>

            {/* Confirm back */}
            <div style={{
              marginTop: 20, padding: '20px 22px', borderRadius: 14,
              background: t.accentSoft, border: `1px solid ${t.accent}30`,
              display: 'flex', gap: 18, alignItems: 'center',
            }}>
              <div style={{ flex: 1 }}>
                <h3 style={{
                  margin: 0, fontFamily: TYPE.display, fontSize: 22, fontWeight: 400,
                  color: t.ink, letterSpacing: -0.3,
                }}>Bought it? Mark it as purchased.</h3>
                <p style={{
                  margin: '4px 0 0', fontFamily: TYPE.body, fontSize: 13.5,
                  color: t.inkSoft, lineHeight: 1.45,
                }}>
                  This releases the timer and lets Ana & Mihai know. Otherwise the item frees up in 29 minutes.
                </p>
              </div>
              <Btn t={t} variant="accent">I completed the purchase ✓</Btn>
            </div>
          </div>

          {/* Sidebar: why the timer */}
          <aside style={{
            padding: 24, background: t.paperDeep, borderRadius: 14, border: `1px solid ${t.line}`,
          }}>
            <span style={{ fontFamily: TYPE.mono, fontSize: 10, color: t.inkFaint, letterSpacing: 1.3, textTransform: 'uppercase' }}>
              How the timer works
            </span>
            <ol style={{
              margin: '16px 0 0', padding: 0, listStyle: 'none',
              display: 'flex', flexDirection: 'column', gap: 14,
            }}>
              {[
                ['Now', 'Nobody else can reserve this gift.'],
                ['You buy it at dedeman.ro', 'Use any payment method. We never see your card.'],
                ['Tap "I completed the purchase"', 'It locks as given, with your name visible only to the owner.'],
                ['Or 30 minutes pass', 'No action, no worries — the item releases and we email you a one-tap re-reserve link.'],
              ].map(([h, b], i) => (
                <li key={i} style={{ display: 'flex', gap: 12 }}>
                  <span style={{
                    width: 22, height: 22, borderRadius: '50%', background: t.ink,
                    color: t.paper, display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontFamily: TYPE.mono, fontSize: 11, flexShrink: 0,
                  }}>{i + 1}</span>
                  <div>
                    <div style={{ fontFamily: TYPE.body, fontSize: 13.5, color: t.ink, fontWeight: 500 }}>{h}</div>
                    <div style={{ fontFamily: TYPE.body, fontSize: 12.5, color: t.inkSoft, marginTop: 2, lineHeight: 1.45 }}>{b}</div>
                  </div>
                </li>
              ))}
            </ol>
          </aside>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// SCREEN 3: Auth — sign in / sign up / guest
// ─────────────────────────────────────────────────────────────
function AuthScreen({ t }) {
  return (
    <div style={{
      background: t.paper, minHeight: '100%', display: 'flex',
    }}>
      {/* Left: form */}
      <div style={{
        flex: '0 0 520px', padding: '56px 56px 40px',
        display: 'flex', flexDirection: 'column',
      }}>
        <Logo t={t} size={22} />
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', maxWidth: 400 }}>
          <span style={{ fontFamily: TYPE.mono, fontSize: 11, color: t.inkFaint, letterSpacing: 1.3, textTransform: 'uppercase' }}>
            You're invited to a registry
          </span>
          <h1 style={{
            margin: '10px 0 8px', fontFamily: TYPE.display, fontSize: 44, fontWeight: 400,
            color: t.ink, letterSpacing: -1, lineHeight: 1.05,
          }}>
            <span style={{ fontStyle: 'italic' }}>Pick up</span> where you left off
          </h1>
          <p style={{
            margin: 0, fontFamily: TYPE.body, fontSize: 14.5, color: t.inkSoft, lineHeight: 1.55,
          }}>
            Sign in so you keep your reservations across devices — or skip ahead as a guest and
            we'll only ask for your name.
          </p>

          {/* Tabs */}
          <div style={{
            display: 'flex', gap: 24, marginTop: 36, borderBottom: `1px solid ${t.line}`,
          }}>
            {['Sign in', 'Create account'].map((tab, i) => (
              <button key={tab} style={{
                padding: '10px 2px', background: 'none', border: 'none',
                borderBottom: `2px solid ${i === 0 ? t.ink : 'transparent'}`,
                marginBottom: -1, fontFamily: TYPE.body, fontSize: 14,
                fontWeight: i === 0 ? 500 : 400, color: i === 0 ? t.ink : t.inkFaint,
                cursor: 'pointer',
              }}>{tab}</button>
            ))}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 16, marginTop: 24 }}>
            <Field t={t} label="Email" value="andrei.popescu@gmail.com" />
            <Field t={t} label="Password" value="••••••••••" suffix={
              <span style={{ fontFamily: TYPE.mono, fontSize: 10, color: t.inkFaint, letterSpacing: 0.8, textTransform: 'uppercase' }}>
                show
              </span>
            } />
            <Btn t={t} variant="primary" size="lg" style={{ width: '100%', marginTop: 6 }}>
              Sign in
            </Btn>
          </div>

          {/* Divider */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 14, margin: '24px 0' }}>
            <div style={{ flex: 1, height: 1, background: t.line }} />
            <span style={{ fontFamily: TYPE.mono, fontSize: 10, color: t.inkFaint, letterSpacing: 1.5, textTransform: 'uppercase' }}>
              or
            </span>
            <div style={{ flex: 1, height: 1, background: t.line }} />
          </div>

          <Btn t={t} variant="ghost" size="lg" style={{ width: '100%' }} icon={
            <span style={{ width: 16, height: 16, borderRadius: '50%', background: `conic-gradient(from 0deg, ${t.accent}, ${t.second}, ${t.accent})`, display: 'inline-block' }} />
          }>
            Continue with Google
          </Btn>

          <div style={{
            marginTop: 28, padding: '18px 20px', background: t.paperDeep,
            borderRadius: 10, border: `1px dashed ${t.line}`,
            display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16,
          }}>
            <div>
              <div style={{ fontFamily: TYPE.body, fontSize: 13.5, fontWeight: 500, color: t.ink }}>
                Just here to reserve a gift?
              </div>
              <div style={{ fontFamily: TYPE.body, fontSize: 12.5, color: t.inkSoft, marginTop: 2 }}>
                Continue as guest — we'll only ask for your name.
              </div>
            </div>
            <Btn t={t} variant="ghost" size="sm">Skip →</Btn>
          </div>
        </div>

        <span style={{ fontFamily: TYPE.mono, fontSize: 10, color: t.inkFaint, letterSpacing: 0.5 }}>
          © poftim 2026 · terms · privacy · en / ro
        </span>
      </div>

      {/* Right: editorial image */}
      <div style={{
        flex: 1, position: 'relative', overflow: 'hidden', background: t.paperDeep,
      }}>
        <img src={PHOTOS.apt} alt="" style={{
          position: 'absolute', inset: 0, width: '100%', height: '100%', objectFit: 'cover',
        }} />
        <div style={{
          position: 'absolute', inset: 0,
          background: `linear-gradient(180deg, transparent 40%, ${t.ink}88 100%)`,
        }} />
        <div style={{
          position: 'absolute', bottom: 40, left: 40, right: 40, color: t.paper,
        }}>
          <p style={{
            margin: 0, fontFamily: TYPE.display, fontSize: 28, fontStyle: 'italic',
            lineHeight: 1.15, letterSpacing: -0.5, textWrap: 'balance',
          }}>
            "Best housewarming gift? Something they'd never buy themselves but secretly wanted."
          </p>
          <span style={{
            display: 'block', marginTop: 12, fontFamily: TYPE.mono, fontSize: 11,
            letterSpacing: 1.2, textTransform: 'uppercase', opacity: 0.8,
          }}>
            — Ioana M., housewarming 2025
          </span>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// SCREEN 4: Guest-to-account conversion (after reserving)
// ─────────────────────────────────────────────────────────────
function GuestConvertScreen({ t }) {
  return (
    <div style={{ background: t.paper, minHeight: '100%', position: 'relative' }}>
      <WebNav t={t} />
      {/* Dimmed registry in background */}
      <div style={{ padding: '40px 40px', opacity: 0.35, pointerEvents: 'none', filter: 'blur(2px)' }}>
        <h1 style={{
          margin: 0, fontFamily: TYPE.display, fontSize: 48, fontWeight: 400,
          color: t.ink, letterSpacing: -1.2, lineHeight: 1,
        }}>Ana & Mihai's housewarming</h1>
        <div style={{
          display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginTop: 32,
        }}>
          {SAMPLE_ITEMS.slice(0, 3).map(it => <ItemCard key={it.id} t={t} item={it} />)}
        </div>
      </div>

      {/* Modal */}
      <div style={{
        position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: 40,
      }}>
        <div style={{
          width: 520, background: t.paper, borderRadius: 20,
          border: `1px solid ${t.line}`,
          boxShadow: '0 40px 80px rgba(0,0,0,0.18)',
          overflow: 'hidden',
        }}>
          <div style={{
            padding: '32px 36px 24px',
            background: `linear-gradient(180deg, ${t.accentSoft}, ${t.paper})`,
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
              <Pill t={t} tone="accent">✓ Reserved</Pill>
              <span style={{ fontFamily: TYPE.mono, fontSize: 11, color: t.inkFaint, letterSpacing: 0.5 }}>
                Sculptural table lamp · 29:58 left
              </span>
            </div>
            <h2 style={{
              margin: 0, fontFamily: TYPE.display, fontSize: 34, fontWeight: 400,
              color: t.ink, letterSpacing: -0.8, lineHeight: 1.05,
            }}>
              Save your spot, <span style={{ fontStyle: 'italic', color: t.accent }}>Andrei</span>?
            </h2>
            <p style={{
              margin: '10px 0 0', fontFamily: TYPE.body, fontSize: 14.5,
              color: t.inkSoft, lineHeight: 1.55,
            }}>
              You've reserved a gift as a guest. Create a free account and we'll keep the
              reservation tied to you across devices — plus send a gentle nudge before the timer runs out.
            </p>
          </div>

          <div style={{ padding: '24px 36px 28px', display: 'flex', flexDirection: 'column', gap: 14 }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {[
                ['🕰', 'Track your 30-minute timer from any device'],
                ['✉', 'Email nudge 5 minutes before it expires'],
                ['🎁', 'See every registry you\'re giving to, in one place'],
              ].map(([ic, txt], i) => (
                <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                  <span style={{
                    width: 28, height: 28, borderRadius: 8, background: t.paperDeep,
                    display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13,
                  }}>{ic}</span>
                  <span style={{ fontFamily: TYPE.body, fontSize: 13.5, color: t.ink }}>{txt}</span>
                </div>
              ))}
            </div>

            <Field t={t} label="Set a password to finish" placeholder="At least 8 characters" />

            <div style={{ display: 'flex', gap: 10, marginTop: 6 }}>
              <Btn t={t} variant="quiet" style={{ flex: 1 }}>Not now, thanks</Btn>
              <Btn t={t} variant="primary" style={{ flex: 1.4 }}>Create my account →</Btn>
            </div>
            <span style={{
              fontFamily: TYPE.mono, fontSize: 10, color: t.inkFaint,
              letterSpacing: 0.5, textAlign: 'center',
            }}>
              Using andrei.popescu@gmail.com from your guest reservation
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────────
// SCREEN 5: Reservation expired + re-reserve
// ─────────────────────────────────────────────────────────────
function ExpiredScreen({ t }) {
  return (
    <div style={{ background: t.paper, minHeight: '100%' }}>
      <WebNav t={t} signedIn />
      <div style={{
        padding: '64px 40px', display: 'grid', gridTemplateColumns: '1fr 420px',
        gap: 48, alignItems: 'center',
      }}>
        <div>
          <span style={{
            display: 'inline-flex', alignItems: 'center', gap: 8,
            padding: '5px 10px', borderRadius: 999, background: t.secondSoft,
            fontFamily: TYPE.mono, fontSize: 11, color: t.second,
            letterSpacing: 1, textTransform: 'uppercase', fontWeight: 500,
          }}>⌛ Reservation expired</span>
          <h1 style={{
            margin: '16px 0 12px', fontFamily: TYPE.display, fontSize: 56, fontWeight: 400,
            color: t.ink, letterSpacing: -1.4, lineHeight: 1,
          }}>
            The timer ran out, <span style={{ fontStyle: 'italic' }}>but the gift's still free.</span>
          </h1>
          <p style={{
            margin: 0, fontFamily: TYPE.body, fontSize: 16, color: t.inkSoft,
            lineHeight: 1.55, maxWidth: 560, textWrap: 'pretty',
          }}>
            Your 30-minute window for the <em style={{ fontFamily: TYPE.display, fontStyle: 'italic', color: t.ink }}>Sculptural table lamp</em> passed
            and we released the reservation at 18:47. Good news — nobody else has grabbed it yet.
          </p>

          <div style={{ display: 'flex', gap: 12, marginTop: 32, flexWrap: 'wrap' }}>
            <Btn t={t} variant="accent" size="lg">Re-reserve now →</Btn>
            <Btn t={t} variant="ghost" size="lg">Back to the full registry</Btn>
          </div>

          <div style={{
            marginTop: 40, padding: '20px 22px', background: t.paperDeep, borderRadius: 12,
            fontFamily: TYPE.body, fontSize: 13, color: t.inkSoft, lineHeight: 1.55,
            maxWidth: 560,
          }}>
            <strong style={{ color: t.ink, fontWeight: 500 }}>Why do we expire reservations? </strong>
            It keeps the registry honest. A 30-minute window is long enough to check out at the
            retailer, short enough that a forgotten tab doesn't block another gift-giver for days.
          </div>
        </div>

        <div style={{
          position: 'relative', borderRadius: 18, overflow: 'hidden',
          border: `1px solid ${t.line}`, background: t.paperDeep,
        }}>
          <div style={{ aspectRatio: '4 / 3', overflow: 'hidden' }}>
            <img src={PHOTOS.lamp} alt="" style={{
              width: '100%', height: '100%', objectFit: 'cover',
              filter: 'grayscale(0.3)',
            }} />
          </div>
          <div style={{ padding: '18px 20px 20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
              <h3 style={{
                margin: 0, fontFamily: TYPE.body, fontSize: 17, fontWeight: 500,
                color: t.ink, letterSpacing: -0.3,
              }}>Sculptural table lamp</h3>
              <span style={{ fontFamily: TYPE.mono, fontSize: 11, color: t.inkFaint, letterSpacing: 0.4 }}>
                dedeman.ro
              </span>
            </div>
            <div style={{
              marginTop: 10, display: 'flex', justifyContent: 'space-between', alignItems: 'center',
            }}>
              <span style={{ fontFamily: TYPE.body, fontSize: 15, color: t.ink, fontWeight: 500 }}>
                420 <span style={{ color: t.inkFaint, fontSize: 11, fontFamily: TYPE.mono }}>RON</span>
              </span>
              <Pill t={t} tone="ok">◯ Available again</Pill>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, {
  WebNav, Logo, Pill, Btn, Field, ItemCard,
  RegistryScreen, ReserveScreen, AuthScreen, GuestConvertScreen, ExpiredScreen,
});
