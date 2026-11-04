import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import axios from "axios";
import {
  FileText,
  LayoutDashboard,
  LogOut,
  Plus,
  Search,
  Eye,
  ArrowLeft,
  ArrowRight,
  Save,
  ShieldCheck,
  AlertTriangle,
  CheckCircle,
  Trash2,
  RefreshCw,
  User as UserIcon,
} from "lucide-react";
import "./style.css";
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api",
});
api.interceptors.request.use((c) => {
  const t = localStorage.getItem("visa_token");
  if (t) c.headers.Authorization = `Bearer ${t}`;
  return c;
});
const tabs = [
  ["visa-details", "Visa details", "visaDetails"],
  ["personal-details", "Personal details", "personalDetails"],
  ["passport-details", "Passport details", "passportDetails"],
  ["address-details", "Address details", "addressDetails"],
  ["security-declaration", "Security declaration", "securityDeclaration"],
];
const COUNTRIES = [
  ["usa", "USA"],
  ["canada", "Canada"],
  ["uk", "UK"],
  ["india", "India"],
];
const fields = {
  "visa-details": [
    {
      k: "visa_category",
      l: "Visa category",
      type: "select",
      opts: ["Non-Immigrant", "Immigrant"],
    },
    {
      k: "embassy",
      l: "Embassy",
      type: "select",
      opts: ["New Delhi", "Hyderabad", "Mumbai", "Pune", "Bangalore"],
    },
    {
      k: "visa_type",
      l: "Visa type",
      type: "select",
      opts: [
        "Tourist",
        "Business",
        "New",
        "Student",
        "Employment",
        "I-Seeter Visiter",
      ],
    },
  ],
  "personal-details": [
    { k: "photo_path", l: "Applicant photo", type: "file", accept: "image/*" },
    { k: "first_name", l: "First name" },
    { k: "last_name", l: "Last name" },
    {
      k: "gender",
      l: "Gender",
      type: "select",
      opts: ["male", "female", "other"],
    },
    { k: "date_of_birth", l: "Date of birth", type: "date" },
    {
      k: "nationality",
      l: "Nationality",
      type: "select",
      opts: ["usa", "canada", "uk", "india"],
    },
    {
      k: "country_of_birth",
      l: "Country of birth",
      type: "select",
      opts: ["usa", "canada", "uk", "india"],
    },
    {
      k: "marital_status",
      l: "Marital status",
      type: "select",
      opts: ["single", "married", "divorced", "widowed"],
    },
    { k: "occupation", l: "Occupation" },
    { k: "email", l: "Email", type: "email" },
    { k: "mobile", l: "Mobile number" },
    { k: "pan_photo", l: "PAN document", type: "file", accept: "image/*" },
    {
      k: "adhar_photo",
      l: "Aadhaar document",
      type: "file",
      accept: "image/*",
    },
  ],
  "passport-details": [
    { k: "passport_number", l: "Passport number" },
    { k: "passport_issued_date", l: "Date of issue", type: "date" },
    { k: "passport_expiry_date", l: "Date of expiry", type: "date" },
    { k: "passport_issued_by", l: "Issuing authority" },
    {
      k: "passport_issued_country",
      l: "Issuing country",
      type: "select",
      opts: ["usa", "canada", "uk", "india"],
    },
    {
      k: "passport_image_path",
      l: "Passport image",
      type: "file",
      accept: "image/*,.pdf",
    },
  ],
  "address-details": [
    { k: "street_address", l: "Street address" },
    { k: "apartment_unit_no", l: "Apartment / Unit number" },
    { k: "city", l: "City" },
    { k: "state_province", l: "State / Province" },
    {
      k: "country",
      l: "Country",
      type: "select",
      opts: ["usa", "canada", "uk", "india"],
    },
    { k: "postal_code", l: "Postal code" },
  ],
  "security-declaration": [
    {
      k: "crime_conviction",
      l: "Have you ever been convicted of a crime?",
      type: "select",
      opts: ["no", "yes"],
    },
    { k: "crime_details", l: "If yes, provide details", type: "textarea" },
    {
      k: "visa_refusal",
      l: "Have you ever been refused a visa?",
      type: "select",
      opts: ["no", "yes"],
    },
    {
      k: "visa_refusal_details",
      l: "If yes, provide country and reason",
      type: "textarea",
    },
    {
      k: "declaration_agreed",
      l: "I confirm that the information provided is true and complete",
      type: "checkbox",
    },
  ],
};
function App() {
  const [user, setUser] = useState(null),
    [page, setPage] = useState("welcome"),
    [apps, setApps] = useState([]),
    [selected, setSelected] = useState(null),
    [tab, setTab] = useState(0),
    [form, setForm] = useState({}),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [adminRows, setAdminRows] = useState([]),
    [stats, setStats] = useState(null),
    [search, setSearch] = useState(""),
    [status, setStatus] = useState("All"),
    [risk, setRisk] = useState("All"),
    [dateFrom, setDateFrom] = useState(""),
    [dateTo, setDateTo] = useState(""),
    [sortBy, setSortBy] = useState("submitted"),
    [resetToken, setResetToken] = useState(""),
    [accountName, setAccountName] = useState(""),
    [accountEmail, setAccountEmail] = useState(""),
    [accountNotice, setAccountNotice] = useState(""),
    [theme, setTheme] = useState(localStorage.getItem("visa_theme") || "light");
  const msg = (e) =>
    e?.response?.data?.message || e?.message || "Something went wrong";
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("visa_theme", theme);
  }, [theme]);
  useEffect(() => {
    const q = new URLSearchParams(window.location.search);
    const verify = q.get("verifyToken"),
      reset = q.get("resetToken");
    if (verify) {
      api
        .post("/auth/verify-email?token=" + encodeURIComponent(verify))
        .then(() => {
          setNotice("Email verified successfully.");
          if (localStorage.getItem("visa_token"))
            api.get("/auth/me").then((r) => {
              setUser(r.data);
              setPage(
                !r.data.emailVerified
                  ? "settings"
                  : r.data.role === "admin"
                    ? "admin"
                    : "dashboard",
              );
            });
        })
        .catch((e) => setError(msg(e)));
      window.history.replaceState({}, "", window.location.pathname);
    }
    if (reset) {
      setResetToken(reset);
      setPage("reset");
      window.history.replaceState({}, "", window.location.pathname);
    }
    if (localStorage.getItem("visa_token"))
      api
        .get("/auth/me")
        .then((r) => {
          setUser(r.data);
          setAccountName(r.data.name);
          setAccountEmail(r.data.email);
          setPage(
            !r.data.emailVerified
              ? "settings"
              : r.data.role === "admin"
                ? "admin"
                : "dashboard",
          );
        })
        .catch(() => {
          localStorage.removeItem("visa_token");
        });
  }, []);
  async function loadApps() {
    const r = await api.get("/applications");
    setApps(r.data);
  }
  async function loadAdmin() {
    const [s, a] = await Promise.all([
      api.get("/admin/dashboard"),
      api.get("/admin/applications"),
    ]);
    setStats(s.data);
    setAdminRows(a.data);
  }
  useEffect(() => {
    if (user?.role === "admin" && page === "admin")
      loadAdmin().catch((e) => setError(msg(e)));
    if (user && user.role !== "admin" && page === "dashboard")
      loadApps().catch((e) => setError(msg(e)));
  }, [user, page]);
  async function auth(e, mode) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const d = Object.fromEntries(new FormData(e.currentTarget));
      const r = await api.post("/auth/" + mode, d);
      localStorage.setItem("visa_token", r.data.token);
      setUser(r.data.user);
      setAccountName(r.data.user.name);
      setAccountEmail(r.data.user.email);
      setPage(
        !r.data.user.emailVerified
          ? "settings"
          : r.data.user.role === "admin"
            ? "admin"
            : "dashboard",
      );
    } catch (x) {
      setError(msg(x));
    } finally {
      setBusy(false);
    }
  }
  function logout() {
    localStorage.removeItem("visa_token");
    setUser(null);
    setPage("login");
    setSelected(null);
  }
  async function forgotPassword(e) {
    e.preventDefault();
    setBusy(true);
    try {
      const d = Object.fromEntries(new FormData(e.currentTarget));
      const r = await api.post("/auth/forgot-password", d);
      setNotice(r.data.message);
      setPage("login");
    } catch (e) {
      setError(msg(e));
    } finally {
      setBusy(false);
    }
  }
  async function resetPassword(e) {
    e.preventDefault();
    setBusy(true);
    try {
      const d = Object.fromEntries(new FormData(e.currentTarget));
      await api.post("/auth/reset-password", {
        token: resetToken,
        password: d.password,
      });
      setNotice("Password reset. Please sign in.");
      setResetToken("");
      setPage("login");
    } catch (e) {
      setError(msg(e));
    } finally {
      setBusy(false);
    }
  }
  async function saveProfile(e) {
    e.preventDefault();
    setBusy(true);
    try {
      const r = await api.patch("/auth/profile", {
        name: accountName,
        email: accountEmail,
      });
      localStorage.setItem("visa_token", r.data.token);
      setUser(r.data.user);
      setNotice(
        "Profile updated. If you changed your email, verify the new address.",
      );
    } catch (e) {
      setError(msg(e));
    } finally {
      setBusy(false);
    }
  }
  async function savePassword(e) {
    e.preventDefault();
    setBusy(true);
    try {
      const d = Object.fromEntries(new FormData(e.currentTarget));
      await api.patch("/auth/password", {
        currentPassword: d.currentPassword,
        newPassword: d.newPassword,
      });
      e.currentTarget.reset();
      setNotice("Password updated successfully.");
    } catch (e) {
      setError(msg(e));
    } finally {
      setBusy(false);
    }
  }
  async function resendVerification() {
    try {
      const r = await api.post("/auth/resend-verification");
      setNotice(r.data.message);
    } catch (e) {
      setError(msg(e));
    }
  }
  async function deleteAccount() {
    const password = window.prompt(
      "Enter your password to permanently delete your account:",
    );
    if (!password) return;
    try {
      await api.delete("/auth/account", { data: { password } });
      logout();
      setNotice("Account deleted.");
    } catch (e) {
      setError(msg(e));
    }
  }
  async function newApp() {
    setBusy(true);
    try {
      const r = await api.post("/applications");
      setSelected(r.data);
      setTab(0);
      setPage(r.data.status === "Draft" ? "editor" : "app-view");
    } catch (e) {
      setError(msg(e));
    } finally {
      setBusy(false);
    }
  }
  async function editApp(a) {
    setBusy(true);
    try {
      const r = await api.get("/applications/" + a.id);
      setSelected(r.data);
      setTab(0);
      setPage(r.data.status === "Draft" ? "editor" : "app-view");
    } catch (e) {
      setError(msg(e));
    } finally {
      setBusy(false);
    }
  }
  function sectionData(t = tab) {
    const key = tabs[t][2];
    return selected?.[key] || {};
  }
  async function saveSection(next = tab) {
    const id = selected.id,
      slug = tabs[next][0],
      key = tabs[next][2];
    setBusy(true);
    setError("");
    try {
      const r = await api.patch(
        `/applications/${id}/sections/${slug}`,
        form[key] || selected[key] || {},
      );
      setSelected(r.data);
      setForm((p) => ({ ...p, [key]: r.data[key] }));
      setNotice("Section saved successfully.");
      if (next < 4) setTab(next + 1);
    } catch (e) {
      setError(msg(e));
    } finally {
      setBusy(false);
    }
  }
  async function upload(field, file) {
    const fd = new FormData();
    fd.append("file", file);
    fd.append("field", field);
    const r = await api.post(`/applications/${selected.id}/files`, fd);
    const key = tabs[tab][2];
    setForm((p) => ({
      ...p,
      [key]: { ...(p[key] || selected[key] || {}), [field]: r.data.path },
    }));
    setNotice(`${file.name} uploaded`);
  }
  async function deleteDraft(id) {
    if (!confirm("Delete this draft application?")) return;
    try {
      await api.delete("/applications/" + id);
      await loadApps();
      setNotice("Draft deleted.");
    } catch (e) {
      setError(msg(e));
    }
  }
  async function submitApp() {
    setBusy(true);
    setError("");
    try {
      const key = "securityDeclaration";
      const saved = await api.patch(
        `/applications/${selected.id}/sections/security-declaration`,
        form[key] || selected[key] || {},
      );
      setSelected(saved.data);
      const r = await api.post(`/applications/${selected.id}/submit`);
      setSelected(r.data);
      setPage("dashboard");
      setNotice("Application submitted successfully.");
    } catch (e) {
      setError(msg(e));
    } finally {
      setBusy(false);
    }
  }
  async function adminDetail(id) {
    const r = await api.get(`/admin/applications/${id}`);
    setSelected(r.data);
    setPage("admin-detail");
  }
  async function adminSave(e) {
    e.preventDefault();
    setBusy(true);
    try {
      const d = Object.fromEntries(new FormData(e.currentTarget));
      await api.patch(`/admin/applications/${selected.id}`, d);
      await adminDetail(selected.id);
      setNotice("Application updated.");
      loadAdmin();
    } catch (e) {
      setError(msg(e));
    } finally {
      setBusy(false);
    }
  }
  async function runAi() {
    setBusy(true);
    setError("");
    try {
      const r = await api.post(`/admin/applications/${selected.id}/trigger-ai`);
      setSelected(r.data);
      setNotice("AI verification completed.");
    } catch (e) {
      setError(msg(e));
    } finally {
      setBusy(false);
    }
  }
  const fileFields = [
    "photo_path",
    "pan_photo",
    "adhar_photo",
    "passport_image_path",
  ];
  async function openFile(path) {
    try {
      const response = await api.get(path.replace(/^\/api/, ""), {
        responseType: "blob",
      });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement("a");
      link.href = url;
      link.target = "_blank";
      link.rel = "noopener noreferrer";
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch (e) {
      setError(msg(e));
    }
  }
  function renderSection(data) {
    return (
      <div className="section-values">
        {Object.entries(data || {}).map(([key, value]) => (
          <div className="detail-row" key={key}>
            <small>{key.replaceAll("_", " ")}</small>
            {fileFields.includes(key) && value ? (
              <button className="text-btn" onClick={() => openFile(value)}>
                View uploaded file <Eye />
              </button>
            ) : ["crime_conviction", "visa_refusal"].includes(key) &&
              (value === true || String(value).toLowerCase() === "yes") ? (
              <span className="security-flag">YES — Review</span>
            ) : (
              <b>
                {value === null || value === undefined || value === ""
                  ? "—"
                  : typeof value === "object"
                    ? JSON.stringify(value)
                    : String(value)}
              </b>
            )}
          </div>
        ))}
      </div>
    );
  }
  const shell = (title, children) => (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <ShieldCheck /> Visa<span>Portal</span>
        </div>
        <button
          className={page === "dashboard" ? "nav active" : "nav"}
          onClick={() =>
            setPage(user?.role === "admin" ? "admin" : "dashboard")
          }
        >
          <LayoutDashboard />
          Dashboard
        </button>
        {user?.role === "admin" && (
          <button
            className={page.startsWith("admin") ? "nav active" : "nav"}
            onClick={() => setPage("admin")}
          >
            <FileText />
            Applications
          </button>
        )}
        <button
          className={page === "settings" ? "nav active" : "nav"}
          onClick={() => setPage("settings")}
        >
          <UserIcon />
          Account settings
        </button>
        <div className="side-bottom">
          <div className="user-chip">
            <UserIcon />
            <div>
              <b>{user?.name}</b>
              <small>{user?.email}</small>
            </div>
          </div>
          <button className="nav" onClick={logout}>
            <LogOut />
            Sign out
          </button>
        </div>
      </aside>
      <main className="main">
        <header>
          <div>
            <small>VISA APPLICATION PORTAL</small>
            <h1>{title}</h1>
          </div>
          <div className="avatar">{user?.name?.[0]?.toUpperCase()}</div>
        </header>
        {error && (
          <div className="alert error">
            {error}
            <button onClick={() => setError("")}>×</button>
          </div>
        )}
        {notice && (
          <div className="alert success">
            {notice}
            <button onClick={() => setNotice("")}>×</button>
          </div>
        )}
        {children}
      </main>
    </div>
  );
  if (!user && page === "welcome")
    return (
      <div className="landing">
        <nav className="landing-nav">
          <div className="brand">
            <ShieldCheck /> Visa<span>Portal</span>
          </div>
          <div>
            <button className="secondary" onClick={() => setPage("login")}>
              Log in
            </button>
            <button className="primary" onClick={() => setPage("register")}>
              Register
            </button>
          </div>
        </nav>
        <section className="landing-hero">
          <div className="landing-copy">
            <span className="eyebrow">SMARTER VISA APPLICATIONS</span>
            <h1>Unlock Global Opportunities with AI-Powered Visa Solutions</h1>
            <p>
              Experience a smarter, faster, and more personalized visa
              application journey. Our intelligent platform guides you every
              step of the way, simplifying complexities and maximizing your
              success.
            </p>
            <button className="primary" onClick={() => setPage("register")}>
              Get Started Now <ArrowRight />
            </button>
          </div>
          <div className="landing-art">
            <div className="passport-art">
              <ShieldCheck />
              <strong>VISA</strong>
              <span>APPLICATION PORTAL</span>
              <div className="passport-lines" />
            </div>
            <div className="orbit orbit-one" />
            <div className="orbit orbit-two" />
          </div>
        </section>
        <section className="landing-about" id="about">
          <span className="eyebrow">OUR VISION</span>
          <h2>Pioneering a Smarter Way to Travel</h2>
          <p>
            At VisaApp, we are leveraging AI to simplify the visa application
            landscape. International travel and relocation should be exciting,
            not encumbered by confusing paperwork.
          </p>
          <p>
            Our mission is to empower individuals and families with an
            intuitive, intelligent and secure platform that makes the
            application journey easier to navigate.
          </p>
        </section>
        <footer className="landing-footer">
          <b>Visa Portal</b>
          <span>AI-assisted visa applications. Human-led decisions.</span>
          <small>
            © {new Date().getFullYear()} Visa Portal. All rights reserved.
          </small>
        </footer>
      </div>
    );
  if (!user)
    return (
      <div className="auth-wrap">
        <div className="auth-card">
          <div className="brand center">
            <ShieldCheck /> Visa<span>Portal</span>
          </div>
          <h1>
            {page === "register"
              ? "Create your account"
              : page === "forgot"
                ? "Reset your password"
                : page === "reset"
                  ? "Choose a new password"
                  : "Welcome back"}
          </h1>
          <p>Manage your visa application securely.</p>
          {error && <div className="alert error">{error}</div>}
          {notice && <div className="alert success">{notice}</div>}
          {page === "forgot" ? (
            <form onSubmit={forgotPassword} className="form-grid">
              <label>
                Email address
                <input type="email" name="email" required />
              </label>
              <button className="primary wide" disabled={busy}>
                Send reset link
              </button>
            </form>
          ) : page === "reset" ? (
            <form onSubmit={resetPassword} className="form-grid">
              <label>
                New password
                <input type="password" name="password" minLength="8" required />
              </label>
              <button className="primary wide" disabled={busy}>
                Reset password
              </button>
            </form>
          ) : (
            <form
              onSubmit={(e) =>
                auth(e, page === "register" ? "register" : "login")
              }
              className="form-grid"
            >
              {page === "register" && (
                <label>
                  Full name
                  <input name="name" required />
                </label>
              )}
              <label>
                Email address
                <input type="email" name="email" required />
              </label>
              <label>
                Password
                <input type="password" name="password" minLength="8" required />
              </label>
              <button className="primary wide" disabled={busy}>
                {busy
                  ? "Please wait…"
                  : page === "register"
                    ? "Create account"
                    : "Sign in"}
              </button>
            </form>
          )}
          {page === "login" && (
            <p className="switch">
              <button
                onClick={() => {
                  setError("");
                  setPage("forgot");
                }}
              >
                Forgot password?
              </button>
            </p>
          )}
          <p className="switch">
            {page === "register" ? "Already registered?" : "New here?"}{" "}
            <button
              onClick={() => {
                setError("");
                setNotice("");
                setPage(page === "register" ? "login" : "register");
              }}
            >
              {page === "register" ? "Sign in" : "Create an account"}
            </button>
          </p>
        </div>
      </div>
    );
  if (page === "settings")
    return shell(
      "Account settings",
      <div className="detail-grid">
        <div className="panel">
          <h2>Profile information</h2>
          <form className="form-grid" onSubmit={saveProfile}>
            <label>
              Full name
              <input
                value={accountName}
                onChange={(e) => setAccountName(e.target.value)}
                required
              />
            </label>
            <label>
              Email address
              <input
                type="email"
                value={accountEmail}
                onChange={(e) => setAccountEmail(e.target.value)}
                required
              />
            </label>
            <button className="primary" disabled={busy}>
              Save profile
            </button>
          </form>
          <div className="detail-row">
            <small>Email verification</small>
            <b>{user.emailVerified ? "Verified" : "Not verified"}</b>
          </div>
          {!user.emailVerified && (
            <button className="secondary" onClick={resendVerification}>
              Resend verification email
            </button>
          )}
        </div>
        <div className="panel">
          <h2>Appearance</h2>
          <label className="theme-setting">
            Color theme
            <select value={theme} onChange={(e) => setTheme(e.target.value)}>
              <option value="light">Light</option>
              <option value="dark">Dark</option>
            </select>
          </label>
        </div>
        <div className="panel">
          <h2>Change password</h2>
          <form className="form-grid" onSubmit={savePassword}>
            <label>
              Current password
              <input type="password" name="currentPassword" required />
            </label>
            <label>
              New password
              <input
                type="password"
                name="newPassword"
                minLength="8"
                required
              />
            </label>
            <button className="primary" disabled={busy}>
              Update password
            </button>
          </form>
          <hr />
          <h3>Delete account</h3>
          <p className="muted">
            Permanently delete your account and associated visa applications.
          </p>
          <button className="secondary danger-text" onClick={deleteAccount}>
            Delete my account
          </button>
        </div>
      </div>,
    );
  if (page === "editor" && selected) {
    const key = tabs[tab][2],
      values = form[key] || selected[key] || {};
    return shell(
      "Visa application",
      <>
        <div className="panel">
          <div className="steps">
            {tabs.map((t, i) => (
              <button
                key={t[0]}
                onClick={() => setTab(i)}
                className={
                  tab === i ? "step current" : i < tab ? "step done" : "step"
                }
              >
                <span>{i < tab ? "✓" : i + 1}</span>
                {t[1]}
              </button>
            ))}
          </div>
          <div className="panel-head">
            <div>
              <h2>{tabs[tab][1]}</h2>
              <p>
                Complete the information below. Your progress is saved section
                by section.
              </p>
            </div>
            <span className="pill">Draft · #{selected.id}</span>
          </div>
          <div className="form-grid two">
            {fields[tabs[tab][0]].map((f) => (
              <label
                className={f.type === "checkbox" ? "check-field" : ""}
                key={f.k}
              >
                {f.type === "checkbox" ? (
                  <>
                    <input
                      type="checkbox"
                      checked={!!values[f.k]}
                      onChange={(e) =>
                        setForm((p) => ({
                          ...p,
                          [key]: { ...values, [f.k]: e.target.checked },
                        }))
                      }
                    />
                    {f.l}
                  </>
                ) : (
                  <>
                    {f.l}
                    {f.type === "select" ? (
                      <select
                        value={
                          ["crime_conviction", "visa_refusal"].includes(f.k)
                            ? values[f.k] === true
                              ? "yes"
                              : values[f.k] === false
                                ? "no"
                                : ""
                            : values[f.k] || ""
                        }
                        onChange={(e) =>
                          setForm((p) => ({
                            ...p,
                            [key]: {
                              ...values,
                              [f.k]: [
                                "crime_conviction",
                                "visa_refusal",
                              ].includes(f.k)
                                ? e.target.value === "yes"
                                : e.target.value,
                            },
                          }))
                        }
                      >
                        <option value="">Select…</option>
                        {f.opts.map((o) => (
                          <option key={o} value={o}>
                            {(
                              COUNTRIES.find((c) => c[0] === o)?.[1] || o
                            ).replace(/\b\w/g, (c) => c.toUpperCase())}
                          </option>
                        ))}
                      </select>
                    ) : f.type === "textarea" ? (
                      <textarea
                        rows="3"
                        value={
                          ["crime_conviction", "visa_refusal"].includes(f.k)
                            ? values[f.k] === true
                              ? "yes"
                              : values[f.k] === false
                                ? "no"
                                : ""
                            : values[f.k] || ""
                        }
                        onChange={(e) =>
                          setForm((p) => ({
                            ...p,
                            [key]: {
                              ...values,
                              [f.k]: [
                                "crime_conviction",
                                "visa_refusal",
                              ].includes(f.k)
                                ? e.target.value === "yes"
                                : e.target.value,
                            },
                          }))
                        }
                      />
                    ) : f.type === "file" ? (
                      <>
                        <input
                          type="file"
                          accept={f.accept}
                          onChange={(e) =>
                            e.target.files[0] &&
                            upload(f.k, e.target.files[0]).catch((x) =>
                              setError(msg(x)),
                            )
                          }
                        />
                        {values[f.k] && (
                          <small className="muted">
                            Uploaded: {String(values[f.k]).split("/").pop()}
                          </small>
                        )}
                      </>
                    ) : (
                      <input
                        type={f.type || "text"}
                        value={
                          ["crime_conviction", "visa_refusal"].includes(f.k)
                            ? values[f.k] === true
                              ? "yes"
                              : values[f.k] === false
                                ? "no"
                                : ""
                            : values[f.k] || ""
                        }
                        onChange={(e) =>
                          setForm((p) => ({
                            ...p,
                            [key]: {
                              ...values,
                              [f.k]: [
                                "crime_conviction",
                                "visa_refusal",
                              ].includes(f.k)
                                ? e.target.value === "yes"
                                : e.target.value,
                            },
                          }))
                        }
                      />
                    )}
                  </>
                )}
              </label>
            ))}
          </div>
          <div className="actions">
            <button
              className="secondary"
              onClick={() => (tab ? setTab(tab - 1) : setPage("dashboard"))}
            >
              <ArrowLeft /> Back
            </button>
            <span className="muted">Step {tab + 1} of 5</span>
            {tab === 4 ? (
              <button className="primary" onClick={submitApp} disabled={busy}>
                Submit application <CheckCircle />
              </button>
            ) : (
              <button
                className="primary"
                onClick={() => saveSection()}
                disabled={busy}
              >
                {busy ? "Saving…" : "Save & continue"} <ArrowRight />
              </button>
            )}
          </div>
        </div>
      </>,
    );
  }
  if (page === "dashboard")
    return shell(
      "My applications",
      <>
        <div className="hero-row">
          <div>
            <h2>Your visa applications</h2>
            <p>Track your application progress and manage your submissions.</p>
          </div>
          <button className="primary" onClick={newApp} disabled={busy}>
            <Plus /> New application
          </button>
        </div>
        <div className="stats">
          <div className="stat">
            <small>Total applications</small>
            <strong>{apps.length}</strong>
          </div>
          <div className="stat">
            <small>Drafts</small>
            <strong>{apps.filter((a) => a.status === "Draft").length}</strong>
          </div>
          <div className="stat">
            <small>Under review</small>
            <strong>
              {
                apps.filter((a) =>
                  ["Submitted", "In Review", "In Progress"].includes(a.status),
                ).length
              }
            </strong>
          </div>
        </div>
        <div className="panel">
          <h2>Application history</h2>
          {apps.length === 0 ? (
            <div className="empty">
              <FileText />
              <h3>No applications yet</h3>
              <p>Start a new visa application to get going.</p>
              <button className="primary" onClick={newApp}>
                <Plus /> Start application
              </button>
            </div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Application ID</th>
                    <th>Created</th>
                    <th>Status</th>
                    <th>AI risk</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {apps.map((a) => (
                    <tr key={a.id}>
                      <td>#{a.id}</td>
                      <td>{a.createdAt?.slice(0, 10) || "—"}</td>
                      <td>
                        <span className="pill">{a.status}</span>
                      </td>
                      <td>{a.aiRiskScore || "—"}</td>
                      <td>
                        {a.status === "Draft" ? (
                          <>
                            <button
                              className="text-btn"
                              onClick={() => editApp(a)}
                            >
                              Continue <ArrowRight />
                            </button>
                            <button
                              className="text-btn danger-text"
                              onClick={() => deleteDraft(a.id)}
                            >
                              <Trash2 />
                            </button>
                          </>
                        ) : (
                          <button
                            className="text-btn"
                            onClick={() => editApp(a)}
                          >
                            View <Eye />
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </>,
    );
  if (page === "admin")
    return shell(
      "Admin dashboard",
      <>
        <div className="stats">
          {[
            ["Total applications", stats?.totalApplications],
            ["Pending review", stats?.pendingReview],
            ["Approved today", stats?.approvedToday],
            ["Rejected today", stats?.rejectedToday],
          ].map(([l, v]) => (
            <div className="stat" key={l}>
              <small>{l}</small>
              <strong>{v ?? "—"}</strong>
            </div>
          ))}
        </div>
        <div className="panel">
          <div className="hero-row">
            <div>
              <h2>Application management</h2>
              <p>
                Review applications, inspect AI findings and record decisions.
              </p>
            </div>
            <button className="secondary" onClick={() => loadAdmin()}>
              <RefreshCw /> Refresh
            </button>
          </div>
          <div className="filters">
            <div className="search">
              <Search />
              <input
                placeholder="Search applicant or application ID"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
            <select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option>All</option>
              {[
                "Draft",
                "Submitted",
                "In Review",
                "In Progress",
                "Requires Attention",
                "Approved",
                "Rejected",
              ].map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
            <select value={risk} onChange={(e) => setRisk(e.target.value)}>
              <option>All</option>
              <option>Low</option>
              <option>Medium</option>
              <option>High</option>
            </select>
            <input
              type="date"
              value={dateFrom}
              onChange={(e) => setDateFrom(e.target.value)}
              aria-label="From date"
            />
            <input
              type="date"
              value={dateTo}
              onChange={(e) => setDateTo(e.target.value)}
              aria-label="To date"
            />
            <select value={sortBy} onChange={(e) => setSortBy(e.target.value)}>
              <option value="submitted">Submission date</option>
              <option value="applicant">Applicant name</option>
              <option value="status">Status</option>
            </select>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Application ID</th>
                  <th>Applicant</th>
                  <th>Email</th>
                  <th>Submitted</th>
                  <th>Status</th>
                  <th>AI risk</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {adminRows
                  .filter(
                    (a) =>
                      (status === "All" || a.status === status) &&
                      (risk === "All" || a.aiRiskScore === risk) &&
                      (!dateFrom ||
                        (a.submittedAt &&
                          a.submittedAt.slice(0, 10) >= dateFrom)) &&
                      (!dateTo ||
                        (a.submittedAt &&
                          a.submittedAt.slice(0, 10) <= dateTo)) &&
                      `${a.applicant?.name} ${a.applicant?.email} ${a.id}`
                        .toLowerCase()
                        .includes(search.toLowerCase()),
                  )
                  .sort((a, b) =>
                    sortBy === "applicant"
                      ? a.applicant.name.localeCompare(b.applicant.name)
                      : sortBy === "status"
                        ? a.status.localeCompare(b.status)
                        : (b.submittedAt || "").localeCompare(
                            a.submittedAt || "",
                          ),
                  )
                  .map((a) => (
                    <tr key={a.id}>
                      <td>#{a.id}</td>
                      <td>{a.applicant?.name}</td>
                      <td>{a.applicant?.email}</td>
                      <td>{a.submittedAt?.slice(0, 10) || "—"}</td>
                      <td>
                        <span className="pill">{a.status}</span>
                      </td>
                      <td>{a.aiRiskScore || "—"}</td>
                      <td>
                        <button
                          className="text-btn"
                          onClick={() => adminDetail(a.id)}
                        >
                          Review <Eye />
                        </button>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>
      </>,
    );
  if (page === "app-view" && selected)
    return shell(
      `Application #${selected.id}`,
      <>
        <button className="back-link" onClick={() => setPage("dashboard")}>
          <ArrowLeft /> Back to dashboard
        </button>
        <div className="panel">
          <div className="hero-row">
            <div>
              <h2>Application status</h2>
              <p>Your submitted visa application and review progress.</p>
            </div>
            <span className="pill">{selected.status}</span>
          </div>
          {selected.applicantFeedback && (
            <div className="alert success">
              Administrator feedback: {selected.applicantFeedback}
            </div>
          )}
          {tabs.map(([slug, label, key]) => (
            <details className="details" open>
              <summary>{label}</summary>
              {renderSection(selected[key])}
            </details>
          ))}
        </div>
      </>,
    );
  if (page === "admin-detail" && selected)
    return shell(
      `Application #${selected.id}`,
      <>
        <button className="back-link" onClick={() => setPage("admin")}>
          <ArrowLeft /> Back to applications
        </button>
        <div className="detail-grid">
          <div className="panel">
            <h2>Applicant details</h2>
            {[
              ["Applicant", selected.applicant?.name],
              ["Email", selected.applicant?.email],
              ["Status", selected.status],
              ["Submitted", selected.submittedAt?.slice(0, 10) || "—"],
            ].map(([k, v]) => (
              <div className="detail-row">
                <small>{k}</small>
                <b>{v || "—"}</b>
              </div>
            ))}
            <h3>Application information</h3>
            {tabs.map(([slug, label, key]) => (
              <details className="details">
                <summary>{label}</summary>
                {renderSection(selected[key])}
              </details>
            ))}
          </div>
          <div className="panel">
            <div className="hero-row">
              <h2>AI verification insights</h2>
              <button className="secondary" onClick={runAi} disabled={busy}>
                <RefreshCw /> {busy ? "Analyzing…" : "Run AI analysis"}
              </button>
            </div>
            <div className="risk-card">
              <small>Overall AI risk</small>
              <strong>{selected.aiRiskScore || "Not analyzed"}</strong>
              <p>
                {selected.aiSummary ||
                  "Run AI analysis to review passport OCR and image checks."}
              </p>
            </div>
            {selected.aiFindings && (
              <>
                <details className="details" open>
                  <summary>Photo verification</summary>
                  {renderSection(selected.aiFindings.image_verification)}
                </details>
                <details className="details" open>
                  <summary>Passport OCR results</summary>
                  {renderSection(
                    selected.aiFindings.passport_ocr?.ocr_data ||
                      selected.aiFindings.passport_ocr,
                  )}
                </details>
                <details className="details" open>
                  <summary>Data consistency checks</summary>
                  {renderSection(selected.aiFindings.data_consistency)}
                </details>
              </>
            )}
            <details className="details">
              <summary>Security declaration flags</summary>
              {renderSection(selected.securityDeclaration)}
            </details>
            {selected.aiFindings && (
              <details className="details">
                <summary>Raw AI response (JSON)</summary>
                <pre>{JSON.stringify(selected.aiFindings, null, 2)}</pre>
              </details>
            )}
            <form onSubmit={adminSave} className="form-grid">
              <label>
                Application status
                <select name="status" defaultValue={selected.status}>
                  {[
                    "Submitted",
                    "In Review",
                    "In Progress",
                    "Requires Attention",
                    "Approved",
                    "Rejected",
                  ].map((s) => (
                    <option key={s}>{s}</option>
                  ))}
                </select>
              </label>
              <label>
                Reason / comments for applicant
                <textarea
                  name="applicantFeedback"
                  defaultValue={selected.applicantFeedback || ""}
                  rows="3"
                  placeholder="Required when rejecting or requesting attention"
                />
              </label>
              <label>
                Internal admin notes
                <textarea
                  name="adminNotes"
                  defaultValue={selected.adminNotes || ""}
                  rows="3"
                />
              </label>
              <button className="primary" disabled={busy}>
                Save decision
              </button>
            </form>
          </div>
        </div>
      </>,
    );
  return shell(
    "Dashboard",
    <div className="panel">Select a page from the navigation.</div>,
  );
}

createRoot(document.getElementById("root")).render(<App />);
