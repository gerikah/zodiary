const screenContent = document.querySelector("#screen-content");
const bottomNav = document.querySelector("#bottom-nav");
const screenTitle = document.querySelector("#screen-title");
const screenButtons = [...document.querySelectorAll("[data-screen]")];
const today = new Date();
today.setHours(12, 0, 0, 0);
const yesterday = new Date(today);
yesterday.setDate(today.getDate() - 1);
const prompts = [
  "What are you learning to give yourself permission to feel?",
  "Which small choice today felt most like you?",
  "What would you like to leave a little more room for?",
];
const state = {
  screen: "home",
  sign: "Libra",
  birthday: "",
  mood: "Hopeful",
  topics: new Set(["Personal growth", "Well-being"]),
  focus: "Reflect",
  reminder: "Evening",
  journalText: "Today felt quieter than I expected, and I made room to notice it.",
  saveMessage: "Draft saved on this device.",
  promptIndex: 0,
  calendarMonth: new Date(today.getFullYear(), today.getMonth(), 1),
  selectedDate: dateKey(yesterday),
};
const appScreens = new Set(["home", "journal", "calendar", "profile", "reading"]);
const titles = {
  welcome: "Welcome",
  birthday: "Birthday",
  sign: "Sign reveal",
  preferences: "Preferences",
  trust: "Trust note",
  home: "Home",
  journal: "Journal",
  calendar: "Calendar",
  profile: "Profile",
  reading: "Daily reading",
};
const views = {
  welcome: '<div class="app-wordmark"><img src="/assets/zodiary-logo.png" alt="Zodiary"></div><div class="spacer-30"></div><h2 class="screen-heading">The universe is<br>listening. What&#39;s<br>your story?</h2><p class="body-copy">The stars inspire. You write.</p><div class="welcome-orbit" aria-hidden="true"><span>✧</span><i></i><b></b></div><div class="spacer-24"></div><button class="primary-button" type="button" data-go="birthday">Begin your story</button>',
  birthday: '<button class="back-button" type="button" data-go="welcome">‹ &nbsp;Back</button><div class="spacer-24"></div><h2 class="screen-heading">Let&#39;s find<br>your sign.</h2><p class="muted-copy">Your birthday is where your story begins.</p><div class="spacer-24"></div><label class="section-label" for="birthday">When is your birthday?</label><div class="spacer-8"></div><input class="form-field" id="birthday" type="date" aria-describedby="birthday-note"><p class="muted-copy" id="birthday-note">We&#39;ll use this to find your zodiac sign. Your full birthday isn&#39;t saved.</p><div class="stars-row" aria-hidden="true">✦ ✧ ✦</div><button class="primary-button" type="button" data-action="discover-sign" disabled>Discover my sign</button>',
  sign: '<button class="back-button" type="button" data-go="birthday">‹ &nbsp;Back</button><div class="spacer-20"></div><h2 class="screen-heading">A new page,<br>in your colors.</h2><div class="sign-medallion" aria-label="{{sign}} zodiac symbol">♎</div><h2 class="display small centered">You&#39;re a {{sign}}!</h2><p class="muted-copy centered">Your Zodiary just took on your colors.</p><div class="spacer-24"></div><button class="primary-button" type="button" data-go="preferences">Explore my sign</button><div class="centered"><button class="text-button" type="button" data-go="birthday">Not quite right? Change my sign</button></div>',
  preferences: '<button class="back-button" type="button" data-go="sign">‹ &nbsp;Back</button><div class="spacer-20"></div><h2 class="screen-heading">Make it personal.</h2><p class="muted-copy">Choose what matters most to you.</p><div class="spacer-20"></div><p class="section-label">What would you like to explore?</p><div class="spacer-8"></div>{{topicOptions}}<div class="spacer-20"></div><p class="section-label">What brings you here?</p><div class="spacer-8"></div>{{focusOptions}}<div class="spacer-20"></div><p class="section-label">Want a daily reminder?</p><div class="spacer-8"></div>{{reminderOptions}}<div class="spacer-20"></div><button class="primary-button" type="button" data-go="trust">Save my preferences</button>',
  trust: '<div class="app-wordmark"><img src="/assets/zodiary-logo.png" alt="Zodiary"></div><div class="spacer-30"></div><h2 class="screen-heading">A note before<br>we begin.</h2><div class="spacer-16"></div><section class="card lavender"><p class="prompt-text trust-quote">The stars offer a perspective; you decide what resonates.</p><p class="body-copy">Readings are for reflection, not prediction or professional advice.</p></section><div class="spacer-24"></div><p class="muted-copy large-copy">Your journal is ready for its first page.</p><div class="spacer-20"></div><button class="primary-button" type="button" data-go="home">Open my journal</button>',
  home: '<div class="app-header"><div class="app-wordmark"><img src="/assets/zodiary-logo.png" alt="Zodiary"></div><button class="sign-chip" type="button" data-go="profile">♎ &nbsp;{{sign}}</button></div><p class="greeting">{{greeting}}, {{sign}}</p><h2 class="display">Your page is<br>waiting.</h2><div class="spacer-16"></div><button class="primary-button" type="button" data-go="journal">Write today&#39;s page</button><div class="spacer-24"></div><section class="card dark"><p class="card-title">Today&#39;s reading for {{sign}}</p><p class="muted-copy dark-copy">A general reading for {{sign}}.</p><p class="body-copy dark-copy">A thoughtful conversation may help you see a familiar choice in a new light.</p><p class="reading-date">Content date · {{todayLong}}</p><p class="attribution">Astrology data by CosmyDay</p><div class="card-action"><button class="text-button" type="button" data-go="reading">Read the full reading →</button></div></section><div class="spacer-14"></div><section class="card form"><p class="card-title">Moon snapshot · {{todayShort}}</p><h3 class="moon-heading">Moon in Gemini</h3><p class="body-copy tight">Waxing Crescent</p><p class="muted-copy tight">34% illuminated</p><p class="attribution light-attribution">Astrology data by CosmyDay</p></section><div class="spacer-16"></div><section class="card"><p class="card-title">A moment to notice</p><p class="prompt-text">{{prompt}}</p><p class="muted-copy">A prompt on Personal growth</p><div class="card-action"><button class="text-button" type="button" data-action="next-prompt">Try another prompt ↗</button></div></section><div class="spacer-16"></div><section class="card lavender"><p class="card-title">Words to carry today</p><p class="affirmation-text">I can meet this day one honest moment at a time.</p></section><div class="spacer-20"></div>',
  journal: '<h2 class="display">Today&#39;s page</h2><p class="muted-copy date-note">{{todayDate}}</p><p class="body-copy strong-copy">How&#39;s your inner universe?</p><p class="muted-copy helper-copy">No right or wrong feelings. Just yours.</p>{{moodOptions}}<div class="spacer-20"></div><section class="card form"><p class="card-title">A prompt on Personal growth</p><p class="body-copy">{{prompt}}</p><div class="card-action"><button class="text-button" type="button" data-action="next-prompt">Try another prompt</button></div></section><div class="spacer-16"></div><textarea class="journal-area" data-journal aria-label="Journal entry" placeholder="Dear universe, today I…">{{journalText}}</textarea><p class="save-note" id="save-note">{{saveMessage}}</p><div class="spacer-12"></div><button class="primary-button" type="button" data-action="save-entry">Save entry</button>',
  calendar: '<h2 class="display">Your story,<br>over time.</h2><p class="muted-copy calendar-intro">A look back at the days you made space to write.</p><section class="month-card"><div class="month-header"><strong>{{month}}</strong><button class="month-control" type="button" data-action="previous-month" aria-label="Previous month">‹</button><button class="month-control" type="button" data-action="next-month" aria-label="Next month">›</button></div><div class="calendar-grid">{{calendarDays}}</div></section><p class="muted-copy calendar-key">Marked days contain a saved reflection.</p><section class="card form"><p class="card-title">{{selectedDate}}</p>{{calendarDetail}}</section>',
  profile: '<h2 class="display">Your space</h2><p class="muted-copy profile-intro">Make this journal feel like yours.</p><section class="profile-sign"><span class="sign-medallion compact-sign" aria-hidden="true">♎</span><div class="profile-detail"><span>Your sign</span><strong>{{sign}}</strong></div><button class="text-button gold-action" type="button" data-go="sign">Change</button></section><div class="spacer-16"></div><div class="profile-row"><div><strong>Your interests</strong><span>{{topics}}</span></div><button class="text-button" type="button" data-go="preferences">Edit</button></div><div class="spacer-12"></div><div class="profile-row"><div><strong>Journal focus</strong><span>{{focus}}</span></div><button class="text-button" type="button" data-go="preferences">Edit</button></div><div class="spacer-12"></div><div class="profile-row"><div><strong>Daily reminder</strong><span>{{reminder}}</span></div><button class="text-button" type="button" data-go="preferences">Edit</button></div><div class="spacer-16"></div><section class="card lavender"><p class="card-title">A note to keep</p><p class="prompt-text">The stars offer a perspective; you decide what resonates.</p><p class="body-copy">Readings are for reflection, not prediction or professional advice.</p></section><div class="spacer-16"></div><section class="card"><p class="card-title">Your account</p><p class="muted-copy">Your journal is saved on this device. Connect Supabase to turn on account sync.</p><p class="save-note">Account actions are disabled in this browser preview.</p></section><div class="spacer-20"></div>',
  reading: '<button class="back-button" type="button" data-go="home">‹ &nbsp;Home</button><div class="spacer-16"></div><h2 class="display">Today&#39;s reading<br>for {{sign}}</h2><p class="muted-copy reading-intro">A general reading for {{sign}}.</p><p class="section-label">Content date · {{todayLong}}</p><h3 class="reading-section-title">Overview</h3><p class="body-copy">A thoughtful conversation may help you see a familiar choice in a new light.</p><h3 class="reading-section-title">Reflection</h3><p class="body-copy">Notice which ideas leave you feeling more grounded and open.</p><h3 class="reading-section-title">A closing thought</h3><p class="body-copy">Let curiosity make a little room for change.</p><p class="attribution light-attribution">Astrology data by CosmyDay</p><p class="muted-copy">The stars offer a perspective; you decide what resonates.</p><p class="muted-copy">Readings are for reflection, not prediction or professional advice.</p><div class="spacer-16"></div><button class="primary-button" type="button" data-go="journal">Write today&#39;s page</button>',
};

function dateKey(date) {
  return date.getFullYear() + "-" + String(date.getMonth() + 1).padStart(2, "0") + "-" + String(date.getDate()).padStart(2, "0");
}
function fromDateKey(value) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day, 12);
}
function formatDate(date, options) {
  return new Intl.DateTimeFormat("en", options).format(date);
}
function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (character) => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  })[character]);
}
function choiceMarkup(items, selected, group, extraClass) {
  return '<div class="choice-grid ' + (extraClass || "") + '">' + items.map((item) => {
    const active = selected.has ? selected.has(item) : selected === item;
    return '<button type="button" class="choice-chip' + (active ? " selected" : "") +
      '" aria-pressed="' + active + '" data-choice-group="' + group + '" data-choice="' + item + '">' +
      (active ? "✓ &nbsp;" : "") + item + "</button>";
  }).join("") + "</div>";
}
function calendarMarkup() {
  const first = new Date(state.calendarMonth.getFullYear(), state.calendarMonth.getMonth(), 1, 12);
  const cursor = new Date(first);
  cursor.setDate(1 - cursor.getDay());
  const headings = ["S", "M", "T", "W", "T", "F", "S"].map((day) => '<span class="weekday">' + day + "</span>").join("");
  const days = Array.from({ length: 42 }, () => {
    const date = new Date(cursor);
    const key = dateKey(date);
    const entry = key === dateKey(yesterday);
    const classes = ["calendar-day", date.getMonth() !== first.getMonth() ? "outside" : "", entry ? "has-entry" : "", key === state.selectedDate ? "selected" : ""].filter(Boolean).join(" ");
    const disabled = date > today ? " disabled" : "";
    cursor.setDate(cursor.getDate() + 1);
    return '<button class="' + classes + '" type="button" data-date="' + key + '" aria-label="' +
      formatDate(date, { month: "long", day: "numeric" }) + '"' + disabled + ">" + date.getDate() + "</button>";
  }).join("");
  return headings + days;
}
function calendarDetail() {
  if (state.selectedDate === dateKey(yesterday)) {
    return '<p class="section-label">Mood · Calm</p><p class="body-copy">I took a slower walk and let the day settle before making a plan.</p><div class="card-action"><button class="text-button" type="button" data-go="journal">Open reflection →</button></div>';
  }
  return '<p class="prompt-text compact-prompt">Every story starts with a blank page.</p><p class="muted-copy">No entry saved for this day yet.</p><div class="card-action"><button class="text-button" type="button" data-go="journal">Write for this day →</button></div>';
}
function greeting() {
  const hour = new Date().getHours();
  return hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
}
function screenMarkup(screen) {
  return views[screen]
    .replaceAll("{{sign}}", escapeHtml(state.sign))
    .replaceAll("{{greeting}}", greeting())
    .replaceAll("{{todayLong}}", formatDate(today, { weekday: "long", month: "long", day: "numeric" }))
    .replaceAll("{{todayDate}}", formatDate(today, { weekday: "long", month: "long", day: "numeric" }))
    .replaceAll("{{todayShort}}", formatDate(today, { month: "long", day: "numeric" }))
    .replaceAll("{{prompt}}", escapeHtml(prompts[state.promptIndex]))
    .replaceAll("{{journalText}}", escapeHtml(state.journalText))
    .replaceAll("{{saveMessage}}", escapeHtml(state.saveMessage))
    .replaceAll("{{month}}", formatDate(state.calendarMonth, { month: "long", year: "numeric" }))
    .replaceAll("{{calendarDays}}", calendarMarkup())
    .replaceAll("{{selectedDate}}", formatDate(fromDateKey(state.selectedDate), { month: "long", day: "numeric" }))
    .replaceAll("{{calendarDetail}}", calendarDetail())
    .replaceAll("{{topicOptions}}", choiceMarkup(["Love", "Career", "Well-being", "Personal growth"], state.topics, "topics"))
    .replaceAll("{{focusOptions}}", choiceMarkup(["Reflect", "Track moods", "Keep memories", "A bit of everything"], state.focus, "focus"))
    .replaceAll("{{reminderOptions}}", choiceMarkup(["Morning", "Evening", "Not now"], state.reminder, "reminder"))
    .replaceAll("{{moodOptions}}", choiceMarkup(["Calm", "Hopeful", "Tender", "Restless", "Heavy"], state.mood, "mood", "mood-grid"))
    .replaceAll("{{topics}}", escapeHtml([...state.topics].join(" · ") || "General prompts"))
    .replaceAll("{{focus}}", escapeHtml(state.focus))
    .replaceAll("{{reminder}}", escapeHtml(state.reminder))
    .replaceAll('id="birthday"', 'id="birthday" value="' + escapeHtml(state.birthday) + '"');
}
function icon(name) {
  const icons = {
    home: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m3.5 10.5 8.5-7 8.5 7v10h-6v-6h-5v6h-6z"/></svg>',
    journal: '<svg viewBox="0 0 24 24" aria-hidden="true"><path d="m4 16.5 11.9-12a2.1 2.1 0 0 1 3 3L7 19.5 3.5 20.5zM13.5 7 17 10.5M4 16.5l3.5 3"/></svg>',
    calendar: '<svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3.5" y="5.5" width="17" height="15" rx="2"/><path d="M7.5 3.5v4M16.5 3.5v4M3.5 10h17"/></svg>',
    profile: '<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4.5 20c.7-3.6 3.6-5.5 7.5-5.5s6.8 1.9 7.5 5.5"/></svg>',
  };
  return icons[name];
}
function render() {
  if (!titles[state.screen]) state.screen = "home";
  screenTitle.textContent = titles[state.screen];
  document.title = "Zodiary · " + titles[state.screen];
  screenContent.className = "screen-content" + (appScreens.has(state.screen) ? "" : " onboarding");
  screenContent.innerHTML = screenMarkup(state.screen);
  screenContent.scrollTop = 0;
  for (const item of screenButtons) item.setAttribute("aria-current", item.dataset.screen === state.screen ? "page" : "false");
  bottomNav.hidden = !appScreens.has(state.screen);
  bottomNav.innerHTML = bottomNav.hidden ? "" : ["home", "journal", "calendar", "profile"].map((name) => {
    const label = name[0].toUpperCase() + name.slice(1);
    const active = state.screen === name || (state.screen === "reading" && name === "home");
    return '<button class="nav-item" type="button" data-screen="' + name + '" aria-current="' + (active ? "page" : "false") + '">' + icon(name) + "<span>" + label + "</span></button>";
  }).join("");
  updateDeviceScale();
}
function updateDeviceScale() {
  const mobile = window.innerWidth <= 700;
  const width = window.innerWidth - (mobile ? 24 : window.innerWidth > 980 ? 500 : 225);
  const height = window.innerHeight - (mobile ? 220 : 190);
  const scale = Math.max(0.56, Math.min(1, width / 393, height / 852));
  document.documentElement.style.setProperty("--phone-scale", String(scale));
}
function zodiacSign(value) {
  const date = new Date(value + "T12:00:00");
  const month = date.getMonth() + 1;
  const day = date.getDate();
  if ((month === 12 && day >= 22) || (month === 1 && day <= 19)) return "Capricorn";
  if ((month === 1 && day >= 20) || (month === 2 && day <= 18)) return "Aquarius";
  if ((month === 2 && day >= 19) || (month === 3 && day <= 20)) return "Pisces";
  if ((month === 3 && day >= 21) || (month === 4 && day <= 19)) return "Aries";
  if ((month === 4 && day >= 20) || (month === 5 && day <= 20)) return "Taurus";
  if ((month === 5 && day >= 21) || (month === 6 && day <= 20)) return "Gemini";
  if ((month === 6 && day >= 21) || (month === 7 && day <= 22)) return "Cancer";
  if ((month === 7 && day >= 23) || (month === 8 && day <= 22)) return "Leo";
  if ((month === 8 && day >= 23) || (month === 9 && day <= 22)) return "Virgo";
  if ((month === 9 && day >= 23) || (month === 10 && day <= 22)) return "Libra";
  if ((month === 10 && day >= 23) || (month === 11 && day <= 21)) return "Scorpio";
  return "Sagittarius";
}
function showToast(message) {
  document.querySelector(".preview-toast")?.remove();
  const toast = document.createElement("div");
  toast.className = "preview-toast";
  toast.setAttribute("role", "status");
  toast.textContent = message;
  document.querySelector("#device").append(toast);
  window.setTimeout(() => toast.remove(), 2600);
}
function onScreenClick(event) {
  const target = event.target.closest("[data-go], [data-action], [data-choice-group], [data-date]");
  if (!target) return;
  if (target.dataset.go) {
    state.screen = target.dataset.go;
    if (state.screen === "journal") state.selectedDate = dateKey(today);
    render();
    return;
  }
  if (target.dataset.choiceGroup) {
    const { choiceGroup, choice } = target.dataset;
    if (choiceGroup === "topics") state.topics.has(choice) ? state.topics.delete(choice) : state.topics.add(choice);
    if (choiceGroup === "focus") state.focus = choice;
    if (choiceGroup === "reminder") state.reminder = choice;
    if (choiceGroup === "mood") state.mood = choice;
    render();
    return;
  }
  if (target.dataset.date) {
    state.selectedDate = target.dataset.date;
    render();
    return;
  }
  const action = target.dataset.action;
  if (action === "discover-sign" && state.birthday) {
    state.sign = zodiacSign(state.birthday);
    state.screen = "sign";
    render();
  } else if (action === "next-prompt") {
    state.promptIndex = (state.promptIndex + 1) % prompts.length;
    render();
  } else if (action === "save-entry") {
    state.saveMessage = "Preview only: no Android entry was saved.";
    render();
    showToast("This sample wasn't saved to the Android app.");
  } else if (action === "previous-month") {
    state.calendarMonth = new Date(state.calendarMonth.getFullYear(), state.calendarMonth.getMonth() - 1, 1);
    render();
  } else if (action === "next-month") {
    state.calendarMonth = new Date(state.calendarMonth.getFullYear(), state.calendarMonth.getMonth() + 1, 1);
    render();
  }
}
document.addEventListener("click", (event) => {
  const target = event.target.closest("[data-screen]");
  if (!target) return;
  state.screen = target.dataset.screen;
  render();
});
screenContent.addEventListener("click", onScreenClick);
screenContent.addEventListener("input", (event) => {
  if (event.target.matches("#birthday")) {
    state.birthday = event.target.value;
    const action = screenContent.querySelector('[data-action="discover-sign"]');
    if (action) action.disabled = !state.birthday;
  } else if (event.target.matches("[data-journal]")) {
    state.journalText = event.target.value;
    state.saveMessage = "Preview text only; not saved to the Android app.";
    const note = screenContent.querySelector("#save-note");
    if (note) note.textContent = state.saveMessage;
    const action = screenContent.querySelector('[data-action="save-entry"]');
    if (action) action.disabled = !state.journalText.trim();
  }
});
window.addEventListener("resize", updateDeviceScale);
render();
