var printSchedule = document.getElementById('print-schedule');
if (printSchedule) {
  printSchedule.addEventListener('click', function () { window.print(); });
}

var photos = document.getElementById('photos');
if (photos) {
  photos.addEventListener('invalid', function () {
    if (!photos.files.length) photos.setCustomValidity('Choose at least one photo.');
  });
  photos.addEventListener('change', function () {
    var remaining = Number(photos.dataset.remaining);
    var count = photos.files.length;
    var selection = document.getElementById('photo-selection');
    selection.textContent = count === 1 ? photos.files[0].name : count + ' photos selected';
    if (!count) selection.textContent = 'No photos selected';
    var message = '';
    if (photos.files.length > remaining) {
      message = 'Choose at most ' + remaining + (remaining === 1 ? ' more photo.' : ' more photos.');
    } else if (Array.from(photos.files).some(function (file) { return file.size > 5 * 1024 * 1024; })) {
      message = 'Each photo must be 5 MB or smaller.';
    }
    photos.setCustomValidity(message);
    if (message) photos.reportValidity();
  });
}

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
var dialog = document.getElementById('leave-dialog');
var pendingLeave = null;

function confirmLeave(action) {
  if (dialog.open) return;
  pendingLeave = action;
  dialog.returnValue = '';
  dialog.showModal();
}

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
    if (dirty && dialog && !form.hasAttribute('data-dirty-guard')) {
      event.preventDefault();
      confirmLeave(function () { form.requestSubmit(event.submitter || undefined); });
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

if (dialog) {
  document.addEventListener('click', function (event) {
    var link = event.target.closest('a');
    if (dirty && link && !link.hash && !event.metaKey && !event.ctrlKey && !event.shiftKey) {
      event.preventDefault();
      confirmLeave(function () { window.location.assign(link.href); });
    }
  });
  dialog.addEventListener('close', function () {
    var action = pendingLeave;
    pendingLeave = null;
    if (dialog.returnValue === 'leave' && action) {
      // The user already confirmed. Allow the original navigation or form POST without a second prompt.
      dirty = false;
      action();
    }
  });
}

var firstInvalid = document.querySelector('[aria-invalid="true"]');
if (firstInvalid) {
  firstInvalid.focus();
}

var deleteDialog = document.getElementById('delete-service-dialog');
var deleteForm = null;
if (deleteDialog) {
  document.querySelectorAll('form[data-delete-service]').forEach(function (form) {
    form.addEventListener('submit', function (event) {
      event.preventDefault();
      deleteForm = form;
      document.getElementById('delete-service-name').textContent = form.dataset.serviceName;
      deleteDialog.returnValue = '';
      deleteDialog.showModal();
    });
  });
  deleteDialog.addEventListener('close', function () {
    if (deleteDialog.returnValue === 'delete' && deleteForm) {
      deleteForm.querySelector('button').disabled = true;
      HTMLFormElement.prototype.submit.call(deleteForm);
    }
    deleteForm = null;
  });
}
