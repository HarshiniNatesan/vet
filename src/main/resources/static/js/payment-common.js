/* Shared helpers for the staff payment UI (doctor, pharmacy, admin, owner). */
(function () {
  var MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  var METHODS = [['CASH','Cash'],['UPI','UPI'],['CARD','Card'],['BANK_TRANSFER','Bank Transfer'],['OTHER','Other']];
  var PAID = ['CONFIRMED','SUCCESS','PAID'];

  function esc(v) {
    return String(v == null ? '' : v).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;');
  }
  // "2026-10-14T23:24:08.043098" -> "14 Oct 2026, 11:24 PM" (no timezone shifting; the server sends local time).
  function fmtDateTime(v) {
    if (!v) return '';
    var m = String(v).match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/);
    if (!m) return String(v);
    var h = Number(m[4]), ap = h >= 12 ? 'PM' : 'AM';
    h = h % 12; if (h === 0) h = 12;
    return Number(m[3]) + ' ' + MONTHS[Number(m[2]) - 1] + ' ' + m[1] + ', ' + h + ':' + m[5] + ' ' + ap;
  }
  function isPaid(status) { return PAID.indexOf(String(status).toUpperCase()) >= 0; }
  function methodText(b) {
    if (!b) return '';
    if (b.paymentMethodLabel) return b.paymentMethodLabel;
    return b.paymentMethod || '';
  }

  function paidSummary(b) {
    var parts = ['<strong>PAID</strong>'];
    if (b.paidAt) parts.push('<span>Paid at: ' + esc(fmtDateTime(b.paidAt)) + '</span>');
    var mt = methodText(b);
    if (mt) parts.push('<span>Method: ' + esc(mt) + '</span>');
    if (b.transactionId) parts.push('<span>Ref: ' + esc(b.transactionId) + '</span>');
    return parts.join(' ');
  }

  /**
   * Render a bill's payment area into `host`.
   *  bill: payment dto; call: function(url, options) -> Promise(json); onUpdate(result) optional.
   * Unpaid bills get an inline form (method, optional reference, Mark as Paid). Paid bills show a PAID indicator.
   * No dialogs, alerts or confirm popups are used; failures show inline.
   */
  function mount(host, bill, call, onUpdate) {
    var uid = 'pay' + bill.id + '-' + Math.floor(Math.random() * 1e6);
    var inv = bill.invoiceNumber || bill.paymentReference || ('#' + bill.id);
    var head = '<div class="pay-head" style="display:flex;gap:12px;flex-wrap:wrap;align-items:center"><b>Invoice: ' + esc(inv) + '</b>' +
               '<span>Amount: ₹' + Number(bill.amount).toFixed(2) + '</span>';
    if (isPaid(bill.status)) { host.innerHTML = head + '<span class="pay-state" style="display:flex;gap:12px;flex-wrap:wrap">' + paidSummary(bill) + '</span></div>'; return; }

    host.innerHTML = head + '<strong class="pay-status">' + esc(bill.status) + '</strong></div>' +
      '<div class="pay-form" style="display:flex;gap:10px;flex-wrap:wrap;align-items:flex-end;margin-top:10px">' +
        '<label style="display:grid;gap:4px">Payment method<select id="' + uid + '-m">' +
          '<option value="">Select method</option>' + METHODS.map(function (x) { return '<option value="' + x[0] + '">' + x[1] + '</option>'; }).join('') +
        '</select></label>' +
        '<label id="' + uid + '-ol" style="display:none;gap:4px">Specify method<input id="' + uid + '-o" type="text" maxlength="60" placeholder="e.g. Cheque"></label>' +
        '<label id="' + uid + '-rl" style="display:grid;gap:4px">Transaction reference (optional)<input id="' + uid + '-r" type="text" maxlength="35" placeholder="UPI / card / bank reference"></label>' +
        '<button type="button" id="' + uid + '-b">Mark as Paid</button>' +
        '<span id="' + uid + '-e" class="error" role="status" aria-live="polite"></span>' +
      '</div>';

    var $ = function (s) { return document.getElementById(uid + '-' + s); };
    var method = $('m'), other = $('o'), ref = $('r'), btn = $('b'), err = $('e');
    method.onchange = function () {
      $('ol').style.display = method.value === 'OTHER' ? 'grid' : 'none';
      var cash = method.value === 'CASH';
      $('rl').style.display = cash ? 'none' : 'grid';
      if (cash) ref.value = '';
    };
    btn.onclick = function () {
      err.textContent = '';
      if (!method.value) { err.textContent = 'Select the payment method.'; return; }
      btn.disabled = true;
      call('/api/payments/' + bill.id + '/mark-paid', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ amount: bill.amount, paymentMethod: method.value, paymentMethodOther: other.value, transactionReference: ref.value })
      }).then(function (result) {
        host.innerHTML = head + '<span class="pay-state" style="display:flex;gap:12px;flex-wrap:wrap">' + paidSummary(result) + '</span></div>';
        if (onUpdate) onUpdate(result);
        window.dispatchEvent(new CustomEvent('payment-status-updated', { detail: { billId: bill.id, status: result.status, amount: result.amount } }));
        try { localStorage.setItem('pawsCarePaymentUpdated', JSON.stringify({ billId: bill.id, status: result.status, at: Date.now() })); } catch (_) {}
      }).catch(function (e) {
        err.textContent = (e && e.message) || 'Unable to update this bill.';
        btn.disabled = false;
      });
    };
  }

  window.PaymentUI = { esc: esc, fmtDateTime: fmtDateTime, isPaid: isPaid, methodText: methodText, mount: mount };
})();
