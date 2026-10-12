document.addEventListener('DOMContentLoaded',()=>{
  const sidebar=document.querySelector('.sidebar');
  document.querySelector('[data-menu]')?.addEventListener('click',()=>sidebar?.classList.toggle('open'));
  document.addEventListener('click',e=>{if(sidebar?.classList.contains('open')&&!sidebar.contains(e.target)&&!e.target.closest('[data-menu]'))sidebar.classList.remove('open')});
  document.querySelectorAll('[data-fill]').forEach(button=>button.addEventListener('click',()=>{
    document.querySelector('input[name="username"]').value=button.dataset.fill;
    document.querySelector('input[name="password"]').value='Library@123';
  }));
  document.querySelectorAll('[data-password-toggle]').forEach(button=>button.addEventListener('click',()=>{
    const input=document.getElementById(button.dataset.passwordToggle);
    const show=input.type==='password';
    input.type=show?'text':'password';
    button.setAttribute('aria-label',show?'Hide password':'Show password');
    button.setAttribute('aria-pressed',String(show));
  }));
  document.querySelectorAll('[data-glow-card]').forEach(card=>{
    card.addEventListener('pointermove',event=>{
      if(event.pointerType==='touch')return;
      const bounds=card.getBoundingClientRect();
      card.style.setProperty('--glow-x',`${event.clientX-bounds.left}px`);
      card.style.setProperty('--glow-y',`${event.clientY-bounds.top}px`);
      card.style.setProperty('--glow-hue',String(30+(event.clientX/window.innerWidth)*28));
      card.classList.add('is-glowing');
    },{passive:true});
    card.addEventListener('pointerleave',()=>card.classList.remove('is-glowing'));
  });
  if(document.querySelector('.app-shell')&&window.matchMedia('(hover:hover) and (pointer:fine)').matches&&!window.matchMedia('(prefers-reduced-motion:reduce)').matches){
    const follower=document.createElement('span');
    follower.className='cursor-follower';
    follower.setAttribute('aria-hidden','true');
    document.body.append(follower);
    let targetX=0,targetY=0,currentX=0,currentY=0,started=false,frameId;
    const render=()=>{
      currentX+=(targetX-currentX)*.16;
      currentY+=(targetY-currentY)*.16;
      follower.style.transform=`translate3d(${currentX-6}px,${currentY-6}px,0)`;
      frameId=window.requestAnimationFrame(render);
    };
    document.addEventListener('pointermove',event=>{
      if(event.pointerType==='touch')return;
      targetX=event.clientX;
      targetY=event.clientY;
      if(!started){
        currentX=targetX;
        currentY=targetY;
        started=true;
        follower.classList.add('is-visible');
        frameId=window.requestAnimationFrame(render);
      }
      const target=event.target instanceof Element?event.target:null;
      follower.classList.toggle('is-over-action',Boolean(target?.closest('a,button,input,select,textarea,summary')));
    },{passive:true});
    document.documentElement.addEventListener('mouseleave',()=>follower.classList.remove('is-visible'));
    document.documentElement.addEventListener('mouseenter',()=>{if(started)follower.classList.add('is-visible')});
    document.addEventListener('visibilitychange',()=>{
      if(document.hidden&&frameId){
        window.cancelAnimationFrame(frameId);
        frameId=undefined;
      }else if(!document.hidden&&started&&!frameId){
        frameId=window.requestAnimationFrame(render);
      }
    });
  }
  document.querySelectorAll('.flash').forEach(el=>setTimeout(()=>{el.style.opacity='0';el.style.transform='translateY(-5px)';setTimeout(()=>el.remove(),250)},4500));
  document.querySelectorAll('form[data-confirm]').forEach(form=>form.addEventListener('submit',e=>{if(!confirm(form.dataset.confirm))e.preventDefault()}));

});

window.addEventListener('load',()=>{
  const loadFonts=()=>{
    const stylesheet=document.createElement('link');
    stylesheet.rel='stylesheet';
    stylesheet.href='https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;600;700&family=Playfair+Display:wght@600;700&display=swap';
    document.head.append(stylesheet);
  };
  if('requestIdleCallback' in window)window.requestIdleCallback(loadFonts,{timeout:2000});
  else setTimeout(loadFonts,0);
},{once:true});
