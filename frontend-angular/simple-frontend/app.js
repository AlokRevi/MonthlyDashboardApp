const API_BASE = "http://localhost:8080/api/v1";
const USE_MOCK = true;

const monthInput = document.getElementById("monthInput");
const yearInput = document.getElementById("yearInput");
const loadDashboardBtn = document.getElementById("loadDashboardBtn");

const monthLabel = document.getElementById("monthLabel");
const currentDateLabel = document.getElementById("currentDateLabel");
const scaleAnchors = document.getElementById("scaleAnchors");
const daysStrip = document.getElementById("daysStrip");
const todayMarker = document.getElementById("todayMarker");
const todayPill = document.getElementById("todayPill");
const categoriesContainer = document.getElementById("categoriesContainer");
const todoList = document.getElementById("todoList");
const todoTitle = document.getElementById("todoTitle");

const MONTH_NAMES = [
  "January", "February", "March", "April", "May", "June",
  "July", "August", "September", "October", "November", "December"
];

init();

function init() {
  const now = new Date();
  yearInput.value = now.getFullYear();

  MONTH_NAMES.forEach((name, index) => {
    const option = document.createElement("option");
    option.value = index + 1;
    option.textContent = name;
    if (index === now.getMonth()) option.selected = true;
    monthInput.appendChild(option);
  });

  loadDashboardBtn.addEventListener("click", loadAll);
  loadAll();
}

async function loadAll() {
  const year = Number(yearInput.value);
  const month = Number(monthInput.value);

  const dashboard = await getMonthlyDashboard(year, month);
  const checklist = await getTodayChecklist();

  renderDashboard(dashboard);
  renderChecklist(checklist);
}

async function getMonthlyDashboard(year, month) {
  if (USE_MOCK) return buildMockDashboard(year, month);

  const res = await fetch(`${API_BASE}/dashboard/monthly?year=${year}&month=${month}`);
  if (!res.ok) throw new Error("Failed to load dashboard");
  return res.json();
}

async function getTodayChecklist() {
  if (USE_MOCK) return buildMockChecklist();

  const res = await fetch(`${API_BASE}/checklist/today`);
  if (!res.ok) throw new Error("Failed to load checklist");
  return res.json();
}

async function markComplete(taskId, occurrenceDate) {
  const todayStr = new Date().toISOString().slice(0, 10);

  if (USE_MOCK) {
    alert(`Mock complete: task ${taskId}, occurrence ${occurrenceDate}`);
    return;
  }

  const res = await fetch(`${API_BASE}/tasks/${taskId}/completions`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      occurrenceDate,
      completionDate: todayStr
    })
  });

  if (!res.ok) {
    const err = await res.json().catch(() => null);
    alert(err?.message || "Could not mark task complete");
    return;
  }

  await loadAll();
}

function renderDashboard(data) {
  monthLabel.textContent = data.monthLabel;
  currentDateLabel.textContent = data.scaleBar.currentDateLabel;

  renderScaleAnchors(data.scaleBar);
  renderDayStrip(data.dayStrip);
  renderCategories(data.categories, data.dayStrip.length);
}

function renderScaleAnchors(scaleBar) {
  scaleAnchors.innerHTML = "";

  const { anchors, lastDay } = scaleBar;

  anchors.forEach((anchorDay) => {
    const percent = ((anchorDay - 1) / Math.max(lastDay - 1, 1)) * 100;

    const anchor = document.createElement("div");
    anchor.className = "anchor";
    anchor.style.left = `${percent}%`;

    anchor.innerHTML = `
      <div class="anchor-line"></div>
      <div class="anchor-label">${anchorDay}</div>
    `;

    scaleAnchors.appendChild(anchor);
  });
}

function renderDayStrip(dayStripData) {
  daysStrip.innerHTML = "";
  daysStrip.style.gridTemplateColumns = `repeat(${dayStripData.length}, 1fr)`;

  let todayIndex = -1;
  let todayDateLabel = "";

  dayStripData.forEach((day, index) => {
    const box = document.createElement("div");
    box.className = "day-box";

    if (day.weekday === "WEDNESDAY") box.classList.add("wednesday");
    if (day.isWeekend) box.classList.add("weekend");
    if (day.isToday) {
      box.classList.add("today");
      todayIndex = index;
      todayDateLabel = formatDateNice(day.date);
    }

    daysStrip.appendChild(box);
  });

  if (todayIndex >= 0) {
    const percent = (todayIndex / Math.max(dayStripData.length - 1, 1)) * 100;
    todayMarker.style.left = `${percent}%`;
    todayPill.textContent = todayDateLabel;
    todayMarker.style.display = "flex";
  } else {
    todayMarker.style.display = "none";
  }
}

function renderCategories(categories, totalDays) {
  categoriesContainer.innerHTML = "";

  if (!categories.length) {
    categoriesContainer.innerHTML = `<p class="empty-state">No categories available.</p>`;
    return;
  }

  categories.forEach((category) => {
    const block = document.createElement("section");
    block.className = "category-block";

    const title = document.createElement("h3");
    title.className = "category-title";
    title.textContent = category.categoryName;
    block.appendChild(title);

    const taskGrid = document.createElement("div");
    taskGrid.className = "task-grid";

    category.tasks.forEach((task) => {
      const row = document.createElement("div");
      row.className = "task-row";

      const name = document.createElement("div");
      name.className = "task-name";
      name.textContent = task.taskName;

      const occurrenceGrid = document.createElement("div");
      occurrenceGrid.className = "occurrence-grid";
      occurrenceGrid.style.gridTemplateColumns = `repeat(${totalDays}, 1fr)`;

      const dayMap = new Map();
      task.occurrences.forEach((occ) => dayMap.set(occ.dayOfMonth, occ));

      for (let day = 1; day <= totalDays; day++) {
        const cell = document.createElement("div");
        cell.className = "occurrence-cell";

        const occurrence = dayMap.get(day);
        if (occurrence) {
          cell.classList.add("visible");

          if (occurrence.completed) {
            cell.classList.add("completed");
            cell.textContent = "X";
          }
        }

        occurrenceGrid.appendChild(cell);
      }

      row.appendChild(name);
      row.appendChild(occurrenceGrid);
      taskGrid.appendChild(row);
    });

    block.appendChild(taskGrid);
    categoriesContainer.appendChild(block);
  });
}

function renderChecklist(data) {
  todoList.innerHTML = "";
  todoTitle.textContent = `Today Checklist · ${formatDateNice(data.today)}`;

  if (!data.items.length) {
    todoList.innerHTML = `<div class="empty-state">Nothing due today 🎉</div>`;
    return;
  }

  data.items.forEach((item) => {
    const card = document.createElement("div");
    card.className = "todo-item";

    const statusClass = item.status === "OVERDUE" ? "status-overdue" : "status-due";

    card.innerHTML = `
      <div class="todo-top">
        <div>
          <div class="todo-task-name">${item.taskName}</div>
          <div class="todo-meta">${item.categoryName} · Due ${formatDateNice(item.occurrenceDate)}</div>
        </div>
        <div class="todo-status ${statusClass}">${formatStatus(item.status)}</div>
      </div>
      <button class="complete-btn">Mark Complete</button>
    `;

    card.querySelector(".complete-btn").addEventListener("click", () => {
      markComplete(item.taskId, item.occurrenceDate);
    });

    todoList.appendChild(card);
  });
}

function formatStatus(status) {
  if (status === "DUE_TODAY") return "Due Today";
  if (status === "OVERDUE") return "Overdue";
  return status;
}

function formatDateNice(dateStr) {
  const d = new Date(dateStr + "T00:00:00");
  const month = MONTH_NAMES[d.getMonth()].slice(0, 3);
  return `${month} ${d.getDate()}, ${d.getFullYear()}`;
}

/* ---------------- MOCK DATA ---------------- */

function buildMockDashboard(year, month) {
  const lastDay = new Date(year, month, 0).getDate();
  const today = new Date();
  const isCurrentMonth =
    today.getFullYear() === year && today.getMonth() + 1 === month;

  const dayStrip = [];
  for (let day = 1; day <= lastDay; day++) {
    const d = new Date(year, month - 1, day);
    const weekdayIndex = d.getDay();
    const weekdayNames = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];

    dayStrip.push({
      date: toDateStr(d),
      dayOfMonth: day,
      weekday: weekdayNames[weekdayIndex],
      isToday: isCurrentMonth && day === today.getDate(),
      isWeekend: weekdayIndex === 0 || weekdayIndex === 6
    });
  }

  return {
    year,
    month,
    monthLabel: `${MONTH_NAMES[month - 1]} ${year}`,
    today: toDateStr(today),
    readOnly: false,
    scaleBar: {
      anchors: [1, 8, 15, 22, lastDay],
      lastDay,
      currentDateLabel: isCurrentMonth
        ? `${MONTH_NAMES[month - 1].slice(0, 3)} ${today.getDate()}`
        : `${MONTH_NAMES[month - 1].slice(0, 3)} ${year}`
    },
    dayStrip,
    categories: [
      {
        categoryId: 1,
        categoryName: "Bills",
        tasks: [
          {
            taskId: 101,
            taskName: "Credit Card Bill",
            recurrenceType: "FIXED_DATE",
            occurrences: [
              { occurrenceDate: `${year}-${pad(month)}-05`, dayOfMonth: 5, completed: true, completionDate: `${year}-${pad(month)}-06`, status: "COMPLETED" },
              { occurrenceDate: `${year}-${pad(month)}-28`, dayOfMonth: 28, completed: false, completionDate: null, status: "UPCOMING" }
            ]
          },
          {
            taskId: 102,
            taskName: "Phone Bill",
            recurrenceType: "FIXED_DATE",
            occurrences: [
              { occurrenceDate: `${year}-${pad(month)}-12`, dayOfMonth: 12, completed: false, completionDate: null, status: "UPCOMING" }
            ]
          }
        ]
      },
      {
        categoryId: 2,
        categoryName: "Plants",
        tasks: [
          {
            taskId: 201,
            taskName: "Plant Watering",
            recurrenceType: "INTERVAL",
            occurrences: [
              { occurrenceDate: `${year}-${pad(month)}-03`, dayOfMonth: 3, completed: true, completionDate: `${year}-${pad(month)}-03`, status: "COMPLETED" },
              { occurrenceDate: `${year}-${pad(month)}-10`, dayOfMonth: 10, completed: false, completionDate: null, status: "OVERDUE" },
              { occurrenceDate: `${year}-${pad(month)}-17`, dayOfMonth: 17, completed: false, completionDate: null, status: "UPCOMING" },
              { occurrenceDate: `${year}-${pad(month)}-24`, dayOfMonth: 24, completed: false, completionDate: null, status: "UPCOMING" }
            ].filter(o => o.dayOfMonth <= lastDay)
          }
        ]
      },
      {
        categoryId: 3,
        categoryName: "Health",
        tasks: [
          {
            taskId: 301,
            taskName: "Medication Refill",
            recurrenceType: "FIXED_DATE",
            occurrences: [
              { occurrenceDate: `${year}-${pad(month)}-18`, dayOfMonth: 18, completed: false, completionDate: null, status: "UPCOMING" }
            ]
          }
        ]
      }
    ]
  };
}

function buildMockChecklist() {
  const today = new Date();
  return {
    today: toDateStr(today),
    items: [
      {
        taskId: 201,
        taskName: "Plant Watering",
        categoryId: 2,
        categoryName: "Plants",
        occurrenceDate: toDateStr(today),
        status: "DUE_TODAY"
      },
      {
        taskId: 101,
        taskName: "Credit Card Bill",
        categoryId: 1,
        categoryName: "Bills",
        occurrenceDate: "2026-04-05",
        status: "OVERDUE"
      }
    ]
  };
}

function toDateStr(date) {
  const y = date.getFullYear();
  const m = pad(date.getMonth() + 1);
  const d = pad(date.getDate());
  return `${y}-${m}-${d}`;
}

function pad(n) {
  return String(n).padStart(2, "0");
}