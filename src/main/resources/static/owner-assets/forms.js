document.querySelectorAll('.day-row').forEach(function (row) {
  var checkbox = row.querySelector('input[type="checkbox"]');
  function updateDay() {
    row.classList.toggle('is-closed', !checkbox.checked);
    row.querySelectorAll('input[type="time"]').forEach(function (input) {
      input.disabled = !checkbox.checked;
    });
  }
  checkbox.addEventListener('change', updateDay);
  updateDay();
});

document.querySelectorAll('textarea').forEach(function (input) {
  function grow() {
    input.style.height = 'auto';
    input.style.height = input.scrollHeight + 'px';
  }
  input.addEventListener('input', grow);
  grow();
});

var dirty = false;
var submitting = false;
document.querySelectorAll('form[data-dirty-guard]').forEach(function (form) {
  form.addEventListener('input', function (event) {
    dirty = true;
    var field = event.target.closest('.field');
    if (field) {
      event.target.removeAttribute('aria-invalid');
      var error = field.querySelector('.field-error');
      if (error) error.textContent = '';
    }
    var summary = form.querySelector('.error-summary');
    if (summary && !form.querySelector('[aria-invalid="true"]') && !summary.querySelector('p')) {
      summary.hidden = true;
    }
  });
  form.addEventListener('change', function () { dirty = true; });
});

document.querySelectorAll('form[data-dirty-guard], form[data-submit-guard]').forEach(function (form) {
  form.addEventListener('submit', function (event) {
    if (submitting) {
      event.preventDefault();
      return;
    }
    submitting = true;
    dirty = false;
    Array.from(form.elements).forEach(function (control) {
      if (control.tagName === 'BUTTON' && control.type === 'submit' && !control.disabled) {
        control.disabled = true;
        control.setAttribute('aria-busy', 'true');
      }
    });
  });
});

document.querySelectorAll('[data-password-toggle]').forEach(function (button) {
  button.addEventListener('click', function () {
    var input = document.getElementById(button.dataset.passwordToggle);
    var visible = input.type === 'password';
    input.type = visible ? 'text' : 'password';
    button.textContent = visible ? 'Hide password' : 'Show password';
    button.setAttribute('aria-pressed', String(visible));
  });
});

window.addEventListener('pageshow', function () {
  submitting = false;
  document.querySelectorAll('button[aria-busy="true"]').forEach(function (button) {
    button.disabled = false;
    button.removeAttribute('aria-busy');
  });
});
window.addEventListener('beforeunload', function (event) {
  if (dirty) {
    event.preventDefault();
    event.returnValue = '';
  }
});

var dialog = document.getElementById('leave-dialog');
var destination = '';
if (dialog) {
  document.addEventListener('click', function (event) {
    var link = event.target.closest('a');
    if (dirty && link && !link.hash && !event.metaKey && !event.ctrlKey && !event.shiftKey) {
      event.preventDefault();
      destination = link.href;
      dialog.showModal();
    }
  });
  dialog.addEventListener('close', function () {
    if (dialog.returnValue === 'leave') {
      dirty = false;
      window.location.assign(destination);
    }
  });
}

var firstInvalid = document.querySelector('[aria-invalid="true"]');
if (firstInvalid) {
  firstInvalid.focus();
}
