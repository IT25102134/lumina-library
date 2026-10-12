document.addEventListener('DOMContentLoaded',()=>{
  const sidebar=document.querySelector('.sidebar');
  document.querySelector('[data-menu]')?.addEventListener('click',()=>sidebar?.classList.toggle('open'));
  document.addEventListener('click',e=>{if(sidebar?.classList.contains('open')&&!sidebar.contains(e.target)&&!e.target.closest('[data-menu]'))sidebar.classList.remove('open')});
  document.querySelectorAll('[data-fill]').forEach(button=>button.addEventListener('click',()=>{
    document.querySelector('input[name="username"]').value=button.dataset.fill;
    document.querySelector('input[name="password"]').value='Library@123';
  }));
  document.querySelectorAll('.flash').forEach(el=>setTimeout(()=>{el.style.opacity='0';el.style.transform='translateY(-5px)';setTimeout(()=>el.remove(),250)},4500));
  document.querySelectorAll('form[data-confirm]').forEach(form=>form.addEventListener('submit',e=>{if(!confirm(form.dataset.confirm))e.preventDefault()}));
  document.querySelectorAll('[data-phone-digits]').forEach(input=>{
    input.addEventListener('input',()=>{input.value=input.value.replace(/\D/g,'').slice(0,10);});
  });
});
