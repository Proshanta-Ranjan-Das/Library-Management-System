// @ts-check
/** @typedef {{id:number,isbn:string,title:string,author:string,category:string,totalCopies:number,availableCopies:number}} Book */
/** @typedef {{id:number,name:string,email:string,phone:string,active:boolean}} Member */
/** @typedef {{id:number,bookTitle:string,memberName:string,borrowDate:string,dueDate:string,returnDate:string|null,status:string,overdue:boolean,fineAmount:number,finePaid:boolean}} Borrow */

const view = /** @type {HTMLElement} */ (document.getElementById('view'));
const state = { tab: 'books', q: '', filter: 'ALL' };
/** @type {Member[]} */ let members = [];

const esc = (/** @type {any} */ s) => String(s ?? '').replace(/[&<>"']/g,
  c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] || c));

async function api(/** @type {string} */ path, method = 'GET', /** @type {any} */ body) {
  const res = await fetch('/api' + path, {
    method, headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined
  });
  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const errs = data && data.errors ? Object.values(data.errors).join(', ') : '';
    throw new Error(errs || (data && data.detail) || res.statusText);
  }
  return data;
}

function toast(/** @type {string} */ msg, ok = true) {
  const t = /** @type {HTMLElement} */ (document.getElementById('toast'));
  t.textContent = msg; t.style.background = ok ? '#16a34a' : '#dc2626'; t.style.display = 'block';
  setTimeout(() => (t.style.display = 'none'), 3500);
}

const views = {
  async books() {
    const page = await api('/books?size=100&q=' + encodeURIComponent(state.q));
    /** @type {Book[]} */ const rows = page.content;
    view.innerHTML = `
      <div class="card"><h2>Add book</h2>
        <form id="f-book">
          <label>ISBN<input name="isbn" required></label>
          <label>Title<input name="title" required></label>
          <label>Author<input name="author" required></label>
          <label>Category<input name="category"></label>
          <label>Copies<input name="totalCopies" type="number" min="1" value="1" required style="min-width:80px"></label>
          <button class="btn">Add</button>
        </form></div>
      <div class="card"><form id="f-search"><label>Search<input name="q" value="${esc(state.q)}" placeholder="title, author, ISBN, category"></label><button class="btn gry">Search</button></form>
        <div class="tbl"><table><tr><th>ID</th><th>Title</th><th>Author</th><th>ISBN</th><th>Category</th><th>Available</th><th></th></tr>
        ${rows.map(b => `<tr><td>${b.id}</td><td>${esc(b.title)}</td><td>${esc(b.author)}</td><td>${esc(b.isbn)}</td><td>${esc(b.category)}</td>
          <td>${b.availableCopies} / ${b.totalCopies}</td>
          <td><button class="btn sm red" data-act="del-book" data-id="${b.id}">Delete</button></td></tr>`).join('')
          || '<tr><td colspan="7" class="muted">No books found</td></tr>'}
        </table></div></div>`;
  },

  async members() {
    const page = await api('/members?size=100');
    members = page.content;
    view.innerHTML = `
      <div class="card"><h2>Add member</h2>
        <form id="f-member">
          <label>Name<input name="name" required></label>
          <label>Email<input name="email" type="email" required></label>
          <label>Phone<input name="phone"></label>
          <button class="btn">Add</button>
        </form></div>
      <div class="card"><div class="tbl"><table><tr><th>ID</th><th>Name</th><th>Email</th><th>Phone</th><th>Status</th><th></th></tr>
        ${members.map(m => `<tr><td>${m.id}</td><td>${esc(m.name)}</td><td>${esc(m.email)}</td><td>${esc(m.phone)}</td>
          <td><span class="tag ${m.active ? 'RETURNED' : 'off'}">${m.active ? 'Active' : 'Inactive'}</span></td>
          <td><button class="btn sm gry" data-act="toggle-member" data-id="${m.id}">${m.active ? 'Deactivate' : 'Activate'}</button>
              <button class="btn sm red" data-act="del-member" data-id="${m.id}">Delete</button></td></tr>`).join('')
          || '<tr><td colspan="6" class="muted">No members</td></tr>'}
        </table></div></div>`;
  },

  async borrowings() {
    const [bookPage, memberPage, rows] = await Promise.all([
      api('/books?size=100'), api('/members?size=100'),
      state.filter === 'OVERDUE' ? api('/borrowings/overdue')
        : api('/borrowings?size=100&sort=id,desc' + (state.filter === 'ALL' ? '' : '&status=' + state.filter)).then(p => p.content)
    ]);
    /** @type {Book[]} */ const books = bookPage.content.filter((/** @type {Book} */ b) => b.availableCopies > 0);
    /** @type {Member[]} */ const active = memberPage.content.filter((/** @type {Member} */ m) => m.active);
    /** @type {Borrow[]} */ const list = rows;
    view.innerHTML = `
      <div class="card"><h2>Borrow a book</h2>
        <form id="f-borrow">
          <label>Book<select name="bookId" required>${books.map(b => `<option value="${b.id}">${esc(b.title)} (${b.availableCopies} left)</option>`).join('')}</select></label>
          <label>Member<select name="memberId" required>${active.map(m => `<option value="${m.id}">${esc(m.name)}</option>`).join('')}</select></label>
          <button class="btn">Borrow</button>
        </form></div>
      <div class="card"><form id="f-filter"><label>Show
          <select name="filter">${['ALL', 'BORROWED', 'RETURNED', 'OVERDUE'].map(f => `<option ${f === state.filter ? 'selected' : ''}>${f}</option>`).join('')}</select></label></form>
        <div class="tbl"><table><tr><th>ID</th><th>Book</th><th>Member</th><th>Borrowed</th><th>Due</th><th>Returned</th><th>Status</th><th>Fine</th><th></th></tr>
        ${list.map(r => `<tr><td>${r.id}</td><td>${esc(r.bookTitle)}</td><td>${esc(r.memberName)}</td><td>${r.borrowDate}</td><td>${r.dueDate}</td><td>${r.returnDate || '-'}</td>
          <td><span class="tag ${r.overdue ? 'late' : r.status}">${r.overdue ? 'OVERDUE' : r.status}</span></td>
          <td>${r.fineAmount > 0 ? r.fineAmount.toFixed(2) + (r.finePaid ? ' (paid)' : '') : '-'}</td>
          <td>${r.status === 'BORROWED' ? `<button class="btn sm grn" data-act="return" data-id="${r.id}">Return</button>` : ''}
              ${r.status === 'RETURNED' && r.fineAmount > 0 && !r.finePaid ? `<button class="btn sm" data-act="pay" data-id="${r.id}">Pay fine</button>` : ''}</td></tr>`).join('')
          || '<tr><td colspan="9" class="muted">No records</td></tr>'}
        </table></div></div>`;
  }
};

async function render() {
  document.querySelectorAll('nav button').forEach(b =>
    b.classList.toggle('on', /** @type {HTMLElement} */ (b).dataset.tab === state.tab));
  try { await views[/** @type {'books'|'members'|'borrowings'} */ (state.tab)](); }
  catch (e) { toast(/** @type {Error} */ (e).message, false); }
}

/** Runs an action, shows success/error, then refreshes the current tab. */
async function run(/** @type {() => Promise<any>} */ fn, /** @type {string} */ okMsg) {
  try { await fn(); toast(okMsg); } catch (e) { toast(/** @type {Error} */ (e).message, false); }
  await render();
}

document.querySelector('nav')?.addEventListener('click', e => {
  const tab = /** @type {HTMLElement} */ (e.target).dataset.tab;
  if (tab) { state.tab = tab; render(); }
});

document.addEventListener('submit', e => {
  e.preventDefault();
  const form = /** @type {HTMLFormElement} */ (e.target);
  const d = /** @type {Record<string,any>} */ (Object.fromEntries(new FormData(form)));
  switch (form.id) {
    case 'f-book': return run(() => api('/books', 'POST', { ...d, totalCopies: Number(d.totalCopies) }), 'Book added');
    case 'f-member': return run(() => api('/members', 'POST', d), 'Member added');
    case 'f-borrow': return run(() => api('/borrowings', 'POST', { bookId: Number(d.bookId), memberId: Number(d.memberId) }), 'Book borrowed');
    case 'f-search': state.q = d.q; return render();
  }
});

document.addEventListener('change', e => {
  const el = /** @type {HTMLSelectElement} */ (e.target);
  if (el.form && el.form.id === 'f-filter') { state.filter = el.value; render(); }
});

document.addEventListener('click', e => {
  const el = /** @type {HTMLElement} */ (e.target);
  const act = el.dataset.act, id = el.dataset.id;
  if (!act) return;
  switch (act) {
    case 'del-book': return confirm('Delete this book?') ? run(() => api('/books/' + id, 'DELETE'), 'Book deleted') : undefined;
    case 'del-member': return confirm('Delete this member?') ? run(() => api('/members/' + id, 'DELETE'), 'Member deleted') : undefined;
    case 'toggle-member': {
      const m = members.find(x => String(x.id) === id);
      return m ? run(() => api('/members/' + id, 'PUT', { name: m.name, email: m.email, phone: m.phone, active: !m.active }), 'Member updated') : undefined;
    }
    case 'return': return run(() => api(`/borrowings/${id}/return`, 'POST'), 'Book returned');
    case 'pay': return run(() => api(`/borrowings/${id}/pay-fine`, 'POST'), 'Fine paid');
  }
});

render();
