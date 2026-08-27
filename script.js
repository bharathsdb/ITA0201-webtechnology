document.addEventListener('DOMContentLoaded', () => {
    const displayBtn = document.getElementById('displayBtn');
    const profileContainer = document.getElementById('profileContainer');

    displayBtn.addEventListener('click', () => {
        const nameInput = document.getElementById('studentName').value.trim();
        const regInput = document.getElementById('registerNumber').value.trim();
        const deptInput = document.getElementById('department').value;
        const yearInput = document.getElementById('yearOfStudy').value;

        if (!nameInput || !regInput || !deptInput || !yearInput) {
            alert('Please fill in all fields before generating the profile.');
            return;
        }

        while (profileContainer.firstChild) {
            profileContainer.removeChild(profileContainer.firstChild);
        }

        const studentCard = document.createElement('div');
        studentCard.classList.add('student-card');

        const profileHeader = document.createElement('div');
        profileHeader.className = 'profile-header';

        const avatar = document.createElement('div');
        avatar.className = 'avatar';
        avatar.textContent = nameInput.charAt(0).toUpperCase();

        const nameRegWrapper = document.createElement('div');

        const profileName = document.createElement('div');
        profileName.className = 'profile-name';
        profileName.textContent = nameInput;

        const profileReg = document.createElement('div');
        profileReg.className = 'profile-reg';
        profileReg.textContent = regInput;

        nameRegWrapper.appendChild(profileName);
        nameRegWrapper.appendChild(profileReg);

        profileHeader.appendChild(avatar);
        profileHeader.appendChild(nameRegWrapper);

        const profileDetails = document.createElement('div');
        profileDetails.className = 'profile-details';

        const deptItem = document.createElement('div');
        deptItem.className = 'detail-item';

        const deptLabel = document.createElement('div');
        deptLabel.className = 'detail-label';
        deptLabel.textContent = 'Department';

        const deptValue = document.createElement('div');
        deptValue.className = 'detail-value';
        deptValue.textContent = deptInput;

        deptItem.appendChild(deptLabel);
        deptItem.appendChild(deptValue);

        const yearItem = document.createElement('div');
        yearItem.className = 'detail-item';

        const yearLabel = document.createElement('div');
        yearLabel.className = 'detail-label';
        yearLabel.textContent = 'Year of Study';

        const yearValue = document.createElement('div');
        yearValue.className = 'detail-value';
        yearValue.textContent = yearInput;

        yearItem.appendChild(yearLabel);
        yearItem.appendChild(yearValue);

        profileDetails.appendChild(deptItem);
        profileDetails.appendChild(yearItem);

        const removeBtn = document.createElement('button');
        removeBtn.className = 'remove-btn';
        removeBtn.textContent = 'Remove Profile';

        removeBtn.addEventListener('click', () => {
            studentCard.remove();
            profileContainer.classList.add('hidden');
        });

        studentCard.appendChild(profileHeader);
        studentCard.appendChild(profileDetails);
        studentCard.appendChild(removeBtn);

        // Add to DOM
        profileContainer.appendChild(studentCard);

        // Ensure container is visible
        profileContainer.classList.remove('hidden');
    });
});
