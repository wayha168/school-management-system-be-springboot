(function () {
    const THEME_KEY = 'sm-theme';
    const LANG_KEY = 'sm-lang';

    const dict = {
        en: {
            'brand.name': 'School Management',
            'brand.console': 'Admin console',
            'nav.overview': 'Overview',
            'nav.manage': 'Manage',
            'nav.access': 'Access',
            'nav.developers': 'Developers',
            'nav.dashboard': 'Dashboard',
            'nav.schools': 'Schools',
            'nav.users': 'Users',
            'nav.classes': 'Classes',
            'nav.scores': 'Scores',
            'nav.grades': 'Grades',
            'nav.attendance': 'Attendance',
            'nav.requests': 'Requests',
            'nav.finance': 'Finance',
            'nav.roles': 'Roles',
            'nav.permissions': 'Permissions',
            'nav.api': 'API Docs',
            'header.online': 'online',
            'header.notifications': 'Notifications',
            'header.profile': 'View my profile',
            'header.dashboard': 'View dashboard',
            'header.logout': 'Logout',
            'header.role': 'Role',
            'header.school': 'School',
            'header.grade': 'Grade',
            'header.classes': 'Classes',
            'header.teaching': 'Teaching',
            'header.room': 'Room',
            'prefs.theme': 'Theme',
            'prefs.theme.light': 'Light',
            'prefs.theme.dark': 'Dark',
            'prefs.lang': 'Language',
            'prefs.lang.en': 'EN',
            'prefs.lang.km': '\u1781\u17d2\u1798\u17c2\u179a',
            'login.title': 'Admin sign in',
            'login.email': 'Email',
            'login.password': 'Password',
            'login.submit': 'Login',
            'login.error': 'Invalid email or password.',
            'login.logout': 'You have been logged out.',
            'common.cancel': 'Cancel',
            'common.confirm': 'Confirm',
            'common.search': 'Search...',
            'common.actions': 'Actions',
            'common.status': 'Status',
            'common.name': 'Name',
            'common.email': 'Email',
            'common.id': 'ID',
            'common.photo': 'Photo',
            'common.live': 'Live',
            'common.add': 'Add',
            'common.back': 'Back',
            'common.save': 'Save',
            'common.filter': 'Filter',
            'common.all': 'All',
            'filter.all': 'All',
            'filter.allRoles': 'All roles',
            'filter.allGrades': 'All grades',
            'filter.allGenerations': 'All generations',
            'filter.allClasses': 'All classes',
            'filter.byLetter': 'Filter by letter',
            'search.nameEmail': 'Search name or email...',
            'search.nameSchool': 'Search name or school...',
            'search.student': 'Search student...',
            'search.generic': 'Search...',
            'notif.title': 'Notifications',
            'notif.subtitle': 'Users, classes & school alerts',
            'notif.unread': 'Unread',
            'notif.read': 'Read',
            'notif.markAll': 'Mark all read',
            'notif.viewAll': 'View all notifications',
            'dash.title': 'Dashboard Overview',
            'dash.subtitle': 'GPA, top students, attendance & term performance.',
            'dash.assignAttendance': 'Assign attendance',
            'dash.schools': 'Schools',
            'dash.classes': 'Classes',
            'dash.students': 'Students',
            'dash.avgGpa': 'Avg GPA',
            'dash.avgPercent': 'Avg score %',
            'dash.withScores': 'Students with scores',
            'dash.attendanceMonth': 'Attendance · 1 month',
            'dash.attendanceMonthHint': 'Present · Absent · Late / Excused',
            'dash.terms': 'Terms · Midterm / Final',
            'dash.termsHint': 'Average percent by term',
            'dash.people': 'People statistics',
            'dash.peopleHint': 'Users, teachers, students and staff',
            'dash.roles': 'Students vs teachers',
            'dash.rolesHint': 'Quick role comparison',
            'dash.topClass': 'Top 1 · by class',
            'dash.topClassHint': 'Highest GPA in each class',
            'dash.topGrade': 'Top 1 · by grade',
            'dash.topGradeHint': 'Highest GPA in each grade',
            'dash.noScores': 'No score data yet.',
            'dash.gpa': 'GPA',
            'dash.percent': '%',
            'dash.letter': 'Letter',
            'dash.student': 'Student',
            'dash.class': 'Class',
            'dash.grade': 'Grade',
            'users.title': 'Users',
            'users.subtitle': 'Manage platform users · real-time online via WebSocket',
            'users.add': 'Add Users',
            'users.role': 'Role',
            'users.grade': 'Grade',
            'users.roomClass': 'Room / Class',
            'users.online': 'Online',
            'users.offline': 'Offline',
            'users.active': 'Active',
            'users.empty': 'No users found.',
            'schools.title': 'Schools',
            'schools.subtitle': 'Manage schools on the platform',
            'schools.add': 'Add School',
            'schools.phone': 'Phone',
            'classes.title': 'Classes',
            'classes.subtitle': 'Manage school classes',
            'classes.add': 'Add Class',
            'attendance.title': 'Attendance',
            'attendance.subtitle': 'Classes · who teaches them · role-scoped records',
            'attendance.assign': 'Assign attendance',
            'attendance.mark': 'Assign attendance',
            'attendance.classes': 'Classes',
            'attendance.records': 'Records',
            'attendance.belongs': 'Belongs to (teachers)',
            'attendance.students': 'Students',
            'grades.title': 'Grades',
            'grades.subtitle': 'Student GPA and subject averages',
            'scores.title': 'Scores',
            'scores.subtitle': 'Pick a class, enter one score per student, then save',
            'scores.period': 'Period',
            'scores.month': 'Month',
            'users.salary': 'Salary',
            'requests.inbox': 'Requests inbox',
            'requests.mine': 'My requests',
            'requests.subtitle': 'Complaints and text requests',
            'requests.allStatuses': 'All statuses',
            'requests.new': 'New request',
            'requests.subject': 'Subject',
            'requests.from': 'From',
            'requests.category': 'Category',
            'requests.created': 'Created',
            'requests.open': 'Open',
            'requests.empty': 'No requests yet.',
            'grades.gpaLocked': 'GPA access required',
            'grades.requestGpa': 'Request GPA access',
            'requests.approveGpa': 'Approve GPA',
            'requests.rejectGpa': 'Reject',
            'finance.title': 'Finance',
            'finance.subtitleStaff': 'Payroll and payments',
            'finance.subtitleMine': 'My account payments',
            'finance.addPayroll': 'Add payroll',
            'finance.addPayment': 'Add payment',
            'finance.payroll': 'Payroll',
            'finance.payments': 'Payments',
            'finance.user': 'User',
            'finance.period': 'Period',
            'finance.amount': 'Amount',
            'finance.note': 'Note',
            'finance.type': 'Type',
            'finance.due': 'Due',
            'finance.emptyPayroll': 'No payroll records.',
            'finance.emptyPayments': 'No payment records.',
            'scores.search': 'Search student or subject...',
            'scores.allSessions': 'All sessions',
            'scores.selectClass': 'Select class',
            'scores.export': 'Export Excel',
            'scores.import': 'Import Excel',
            'scores.add': 'Add one score',
            'scores.sessionBtn': 'Input score',
            'scores.inputTitle': 'Input scores',
            'scores.inputSubtitle': 'Enter a score for each student, then save. Records are saved per subject and period.',
            'scores.sessionOpen': 'Open class score sheet',
            'scores.sessionTitle': 'Enter class scores',
            'scores.sessionSubtitle': 'Show every student in the class, type scores, then save all at once',
            'scores.sessionHint': 'Fill any cells you need, then press Save all scores once.',
            'scores.pickHint': 'Select a session and class to load score rows for that class.',
            'scores.session': 'Session',
            'scores.subject': 'Subject',
            'scores.allSubjects': 'All subjects',
            'scores.subjectPlaceholder': 'e.g. Math',
            'scores.term': 'Term',
            'scores.score': 'Score',
            'scores.maxScore': 'Max score',
            'scores.remark': 'Remark',
            'scores.remarkOptional': 'Optional',
            'scores.loadSheet': 'Load table',
            'scores.saveSession': 'Save all scores',
            'scores.noStudents': 'No students in this class.',
            'scores.sessionPickClass': 'Select a class to load every student in that class.',
            'scores.sessionPickSubject': 'Pick a subject (or All subjects) to load the score sheet.',
            'scores.noClassSubjects': 'This class has no subjects yet.',
            'scores.editClassSubjects': 'Add subjects to class',
            'scores.teacher': 'Teacher',
            'scores.empty': 'No scores for this class/session.',
        },
        km: {
            'brand.name': '\u1782\u17d2\u179a\u1794\u17cb\u1782\u17d2\u179a\u1784\u179f\u17b6\u179b\u17b6',
            'brand.console': '\u1795\u17d2\u1791\u17b6\u17c6\u1784\u1782\u17d2\u179a\u1794\u17cb\u1782\u17d2\u179a\u1784',
            'nav.overview': '\u1791\u17b7\u178a\u17d2\u1792\u1797\u17b6\u1796\u1791\u17bc\u1791\u17bc',
            'nav.manage': '\u1782\u17d2\u179a\u1794\u17cb\u1782\u17d2\u179a\u1784',
            'nav.access': '\u179f\u17b7\u1791\u17d2\u1792\u17b7\u1785\u17bc\u179b',
            'nav.developers': '\u17a2\u17d2\u1793\u1780\u17a2\u1797\u17b7\u179c\u17b8\u178c\u17d2\u1793\u17cd',
            'nav.dashboard': '\u1795\u17d2\u1791\u17b6\u17c6\u1784\u1782\u17d2\u179a\u1794\u17cb\u1782\u17d2\u179a\u1784',
            'nav.schools': '\u179f\u17b6\u179b\u17b6',
            'nav.users': '\u17a2\u17d2\u1793\u1780\u1794\u17d2\u179a\u17be',
            'nav.classes': '\u1790\u17d2\u1793\u17b6\u1780\u17cb',
            'nav.scores': '\u1796\u17b7\u1793\u17d2\u1791\u17bb',
            'nav.grades': '\u1793\u17b7\u1791\u17d2\u1791\u17c1\u179f',
            'nav.attendance': '\u179c\u178f\u17d2\u178f\u1798\u17b6\u1793',
            'nav.requests': '\u179f\u17c6\u178e\u17be',
            'nav.finance': '\u17a0\u17b7\u179a\u1789\u17d2\u1789\u179c\u178f\u17d2\u1790\u17bb',
            'nav.roles': '\u178f\u17bd\u1793\u17b6\u1791\u17b8',
            'nav.permissions': '\u179f\u17b7\u1791\u17d2\u1792\u17b7',
            'nav.api': '\u17a2\u17c1\u1780\u179f\u17b6\u179a API',
            'header.online': '\u17a2\u1793\u17a1\u17b6\u1789',
            'header.notifications': '\u1780\u17b6\u179a\u1787\u17bc\u1793\u178a\u17c6\u178e\u17b9\u1784',
            'header.profile': '\u1798\u17be\u179b\u1794\u17d2\u179a\u179c\u178f\u17d2\u178f\u17b7\u179a\u17bc\u1794',
            'header.dashboard': '\u1798\u17be\u179b\u1795\u17d2\u1791\u17b6\u17c6\u1784\u1782\u17d2\u179a\u1794\u17cb\u1782\u17d2\u179a\u1784',
            'header.logout': '\u1785\u17b6\u1780\u1785\u17c1\u1789',
            'header.role': '\u178f\u17bd\u1793\u17b6\u1791\u17b8',
            'header.school': '\u179f\u17b6\u179b\u17b6',
            'header.grade': '\u1790\u17d2\u1793\u17b6\u1780\u17cb\u1791\u17b8',
            'header.classes': '\u1790\u17d2\u1793\u17b6\u1780\u17cb',
            'header.teaching': '\u1794\u1784\u17d2\u179a\u17c0\u1793',
            'header.room': '\u1794\u1793\u17d2\u1791\u1794\u17cb',
            'prefs.theme': '\u179a\u17bc\u1794\u179a\u17b6\u1784',
            'prefs.theme.light': '\u1797\u17d2\u179b\u17ba',
            'prefs.theme.dark': '\u1784\u1784\u17b9\u178f',
            'prefs.lang': '\u1797\u17b6\u179f\u17b6',
            'prefs.lang.en': 'EN',
            'prefs.lang.km': '\u1781\u17d2\u1798\u17c2\u179a',
            'login.title': '\u1785\u17bc\u179b\u1794\u17d2\u179a\u17be\u1794\u17d2\u179a\u17b6\u179f\u17cb',
            'login.email': '\u17a2\u17ca\u17b8\u1798\u17c1\u179b',
            'login.password': '\u1796\u17b6\u1780\u17d2\u1799\u179f\u1798\u17d2\u1784\u17b6\u178f\u17cb',
            'login.submit': '\u1785\u17bc\u179b',
            'login.error': '\u17a2\u17ca\u17b8\u1798\u17c1\u179b \u17a2\u17bc \u1796\u17b6\u1780\u17d2\u1799\u179f\u1798\u17d2\u1784\u17b6\u178f\u17cb\u1798\u17b7\u1793\u178f\u17d2\u179a\u17b9\u1798\u178f\u17d2\u179a\u17bc\u179c\u17cb\u1797',
            'login.logout': '\u17a2\u17d2\u1793\u1780\u1794\u17b6\u1793\u1785\u17b6\u1780\u1785\u17c1\u1789\u17a0\u17be\u1799\u17d4',
            'common.cancel': '\u1794\u17c9\u17b6\u17c7\u1794\u1784\u17cb',
            'common.confirm': '\u1794\u1789\u17d2\u1787\u17b6\u1780\u17cb',
            'common.search': '\u179f\u17d2\u179c\u17c2\u1784\u179a\u1780...',
            'common.actions': '\u179f\u1780\u1798\u17d2\u1798\u1797\u17b6\u1796',
            'common.status': '\u179f\u1790\u17b6\u1797\u17b6\u1796',
            'common.name': '\u17a2\u17d2\u1793\u1780',
            'common.email': '\u17a2\u17ca\u17b8\u1798\u17c1\u179b',
            'common.id': '\u17a2\u17b6\u179f\u1799\u179a\u1793\u17cd',
            'common.photo': '\u179a\u17bc\u1794\u1797\u17b6\u1796',
            'common.live': '\u1795\u17d2\u1791\u17b6\u17c6\u1784\u1795\u17d2\u1791\u17b7\u179f\u17cb',
            'common.add': '\u1794\u1793\u17d2\u1790\u17c3\u1798',
            'common.back': 'ថោយក្រោយ',
            'common.save': '\u179a\u1780\u17d2\u179f\u17b6\u1791\u17bb\u1780',
            'common.filter': '\u178f\u179a\u17bd\u179c',
            'common.all': '\u1791\u17b6\u17c6\u1784\u17a2\u179f\u17cb',
            'filter.all': '\u1791\u17b6\u17c6\u1784\u17a2\u179f\u17cb',
            'filter.allRoles': '\u178f\u17bd\u1793\u17b6\u1791\u17b8\u1791\u17b6\u17c6\u1784\u17a2\u179f\u17cb',
            'filter.allGrades': '\u1790\u17d2\u1793\u17b6\u1780\u17cb\u1791\u17b8\u1791\u17b6\u17c6\u1784\u17a2\u179f\u17cb',
            'filter.allGenerations': '\u1787\u17b7\u1793\u17d2\u179b\u17b6\u1793\u1791\u17b6\u17c6\u1784\u17a2\u179f\u17cb',
            'filter.allClasses': '\u1790\u17d2\u1793\u17b6\u1780\u17cb\u1791\u17b6\u17c6\u1784\u17a2\u179f\u17cb',
            'filter.byLetter': '\u178f\u179a\u17bd\u179c\u178f\u17b6\u1798\u17a2\u1780\u17d2\u179f\u179a',
            'search.nameEmail': '\u179f\u17d2\u179c\u17c2\u1784\u179a\u1780\u17a2\u17d2\u1793\u1780 \u17a2\u17bc \u17a2\u17ca\u17b8\u1798\u17c1\u179b...',
            'search.nameSchool': '\u179f\u17d2\u179c\u17c2\u1784\u179a\u1780\u17a2\u17d2\u1793\u1780 \u17a2\u17bc \u179f\u17b6\u179b\u17b6...',
            'search.student': '\u179f\u17d2\u179c\u17c2\u1784\u179a\u1780\u179f\u17b7\u179f\u17d2\u179f\u17b8...',
            'search.generic': '\u179f\u17d2\u179c\u17c2\u1784\u179a\u1780...',
            'notif.title': '\u1780\u17b6\u179a\u1787\u17bc\u1793\u178a\u17c6\u178e\u17b9\u1784',
            'notif.subtitle': '\u17a2\u17d2\u1793\u1780\u1794\u17d2\u179a\u17be \u1790\u17d2\u1793\u17b6\u1780\u17cb \u1793\u17b7\u1784\u179f\u17b6\u179b\u17b6',
            'notif.unread': '\u1798\u17b7\u1793\u1791\u17b6\u1793\u17cb\u17a2\u17b6\u1793',
            'notif.read': '\u1794\u17b6\u1793\u17a2\u17b6\u1793',
            'notif.markAll': '\u179f\u1798\u17d2\u1782\u17b6\u179b\u17cb\u1790\u17b6\u17a2\u17b6\u1793\u1791\u17b6\u17c6\u1784\u17a2\u179f\u17cb',
            'notif.viewAll': '\u1798\u17be\u179b\u1791\u17b6\u17c6\u1784\u17a2\u179f\u17cb',
            'dash.title': '\u1791\u17b7\u178a\u17d2\u1792\u1797\u17b6\u1796\u1795\u17d2\u1791\u17b6\u17c6\u1784\u1782\u17d2\u179a\u1794\u17cb\u1782\u17d2\u179a\u1784',
            'dash.subtitle': 'GPA \u179f\u17b7\u179f\u17d2\u179f\u17b8\u179b\u17be\u1780 \u179c\u178f\u17d2\u178f\u1798\u17b6\u1793 \u1793\u17b7\u1784\u1796\u17b7\u1793\u17d2\u1791\u17bb\u1796\u17b6\u1780\u17cb\u179f\u17b7\u179f\u17d2\u179f\u17b8\u17d4',
            'dash.assignAttendance': '\u1780\u17c6\u178e\u178f\u17cb\u179c\u178f\u17d2\u178f\u1798\u17b6\u1793',
            'dash.schools': '\u179f\u17b6\u179b\u17b6',
            'dash.classes': '\u1790\u17d2\u1793\u17b6\u1780\u17cb',
            'dash.students': '\u179f\u17b7\u179f\u17d2\u179f\u17b8',
            'dash.avgGpa': 'GPA \u1798\u17b7\u1787\u17d2\u1787\u1798\u17b6\u1793',
            'dash.avgPercent': '\u1796\u17b7\u1793\u17d2\u1791\u17bb % \u1798\u17b7\u1787\u17d2\u1787\u1798\u17b6\u1793',
            'dash.withScores': '\u179f\u17b7\u179f\u17d2\u179f\u17b8\u1798\u17b6\u1793\u1796\u17b7\u1793\u17d2\u1791\u17bb',
            'dash.attendanceMonth': '\u179c\u178f\u17d2\u178f\u1798\u17b6\u1793 · \u17f1 \u1781\u17c2',
            'dash.attendanceMonthHint': '\u179c\u178f\u17d2\u178f\u1798\u17b6\u1793 · \u17a2\u179c\u178f\u17d2\u178f\u1798\u17b6\u1793 · \u1799\u17b6\u1799 / \u17a2\u1793\u17bb\u1789\u17d2\u1789\u17b6\u178f',
            'dash.terms': '\u1796\u17b6\u1780\u17cb\u179f\u17b7\u179f\u17d2\u179f\u17b8 · \u1796\u17b6\u1780\u17cb\u1780\u17d2\u1793\u17bb\u1784 / \u1795\u17d2\u1791\u17b6\u17c6\u1784',
            'dash.termsHint': '\u1797\u17b6\u1780\u1795\u17d2\u1791\u17bb\u1780\u17d2\u179a\u17bb\u1784\u1798\u17b7\u1787\u17d2\u1787\u1798\u17b6\u1793\u178f\u17b6\u1798\u1796\u17b6\u1780\u17cb',
            'dash.people': '\u179f\u1790\u17b7\u1791\u17b7\u179f\u17d2\u1790\u17b7\u1798\u17b6\u1793\u17bb\u179f\u17d2\u179f',
            'dash.peopleHint': '\u17a2\u17d2\u1793\u1780\u1794\u17d2\u179a\u17be \u1782\u17d2\u179a\u17bc\u179f\u17b6\u179f\u17d2\u179a\u17cd \u179f\u17b7\u179f\u17d2\u179f\u17b8 \u1793\u17b7\u1784\u1794\u17bb\u1782\u17d2\u1782\u179b\u17b7\u1780\u1780\u17b6\u179a',
            'dash.roles': '\u179f\u17b7\u179f\u17d2\u179f\u17b8 \u1793\u17b7\u1784\u1782\u17d2\u179a\u17bc\u179f\u17b6\u179f\u17d2\u179a\u17cd',
            'dash.rolesHint': '\u1794\u17d2\u179a\u17b8\u1794\u1792\u17b6\u1794\u17cb\u178f\u17bd\u1793\u17b6\u1791\u17b8',
            'dash.topClass': '\u1791\u17b8\u17f1 · \u178f\u17b6\u1798\u1790\u17d2\u1793\u17b6\u1780\u17cb',
            'dash.topClassHint': 'GPA \u1781\u17d2\u1798\u17bb\u1793\u179f\u17bb\u1784\u1794\u17c6\u1796\u17bb\u1780\u1790\u17d2\u1793\u17b6\u1780\u17cb',
            'dash.topGrade': '\u1791\u17b8\u17f1 · \u178f\u17b6\u1798\u1790\u17d2\u1793\u17b6\u1780\u17cb\u1791\u17b8',
            'dash.topGradeHint': 'GPA \u1781\u17d2\u1798\u17bb\u1793\u179f\u17bb\u1784\u1794\u17c6\u1796\u17bb\u1780\u1790\u17d2\u1793\u17b6\u1780\u17cb\u1791\u17b8',
            'dash.noScores': '\u1798\u17b7\u1793\u1791\u17b6\u1793\u17cb\u1798\u17b6\u1793\u1791\u17b7\u1793\u17d2\u1793\u17cd\u1796\u17b7\u1793\u17d2\u1791\u17bb\u1791\u17b6\u17c5\u17d4',
            'dash.gpa': 'GPA',
            'dash.percent': '%',
            'dash.letter': '\u1791\u17b7\u1793\u17d2\u1793\u17cd',
            'dash.student': '\u179f\u17b7\u179f\u17d2\u179f\u17b8',
            'dash.class': '\u1790\u17d2\u1793\u17b6\u1780\u17cb',
            'dash.grade': '\u1790\u17d2\u1793\u17b6\u1780\u17cb\u1791\u17b8',
            'users.title': '\u17a2\u17d2\u1793\u1780\u1794\u17d2\u179a\u17be',
            'users.subtitle': '\u1782\u17d2\u179a\u1794\u17cb\u1782\u17d2\u179a\u1784\u17a2\u17d2\u1793\u1780\u1794\u17d2\u179a\u17be · \u17a2\u1793\u17a1\u17b6\u1789\u1795\u17d2\u1791\u17b7\u179f\u17cb\u178f\u17b6\u1798 WebSocket',
            'users.add': '\u1794\u1793\u17d2\u1790\u17c3\u1798\u17a2\u17d2\u1793\u1780\u1794\u17d2\u179a\u17be',
            'users.role': '\u178f\u17bd\u1793\u17b6\u1791\u17b8',
            'users.grade': '\u1790\u17d2\u1793\u17b6\u1780\u17cb\u1791\u17b8',
            'users.roomClass': '\u1794\u1793\u17d2\u1791\u1794\u17cb / \u1790\u17d2\u1793\u17b6\u1780\u17cb',
            'users.online': '\u17a2\u1793\u17a1\u17b6\u1789',
            'users.offline': '\u1780\u17d2\u1793\u17bb\u1784\u17a2\u1793\u17a1\u17b6\u1789',
            'users.active': '\u179f\u1780\u1798\u17d2\u1798\u1797\u17b6\u1796',
            'users.empty': '\u179a\u1780\u179a\u17be\u1784\u17a2\u17d2\u1793\u1780\u1794\u17d2\u179a\u17be\u17d4',
            'schools.title': '\u179f\u17b6\u179b\u17b6',
            'schools.subtitle': '\u1782\u17d2\u179a\u1794\u17cb\u1782\u17d2\u179a\u1784\u179f\u17b6\u179b\u17b6\u1793\u17c5\u179c\u17c2\u179a\u1794\u17d2\u179a\u1796\u17d0\u1793\u17d2\u1792',
            'schools.add': '\u1794\u1793\u17d2\u1790\u17c3\u1798\u179f\u17b6\u179b\u17b6',
            'schools.phone': '\u179b\u17c1\u1781',
            'classes.title': '\u1790\u17d2\u1793\u17b6\u1780\u17cb',
            'classes.subtitle': '\u1782\u17d2\u179a\u1794\u17cb\u1782\u17d2\u179a\u1784\u1790\u17d2\u1793\u17b6\u1780\u17cb\u1793\u17c5\u179f\u17b6\u179b\u17b6',
            'classes.add': '\u1794\u1793\u17d2\u1790\u17c3\u1798\u1790\u17d2\u1793\u17b6\u1780\u17cb',
            'attendance.title': '\u179c\u178f\u17d2\u178f\u1798\u17b6\u1793',
            'attendance.subtitle': '\u1790\u17d2\u1793\u17b6\u1780\u17cb · \u1782\u17d2\u179a\u17bc\u179f\u17b6\u179f\u17d2\u179a\u17cd · \u1780\u17c6\u178e\u178f\u17cb\u1791\u17b6\u1798\u178f\u17bd\u1793\u17b6\u1791\u17b8',
            'attendance.assign': '\u1780\u17c6\u178e\u178f\u17cb\u179c\u178f\u17d2\u178f\u1798\u17b6\u1793',
            'attendance.mark': '\u1780\u17c6\u178e\u178f\u17cb\u179c\u178f\u17d2\u178f\u1798\u17b6\u1793',
            'attendance.classes': '\u1790\u17d2\u1793\u17b6\u1780\u17cb',
            'attendance.records': '\u1780\u17c6\u178e\u178f\u17cb\u178f\u17b6\u179a\u17b6\u1784',
            'attendance.belongs': '\u1787\u17b6\u1780\u17cb\u1785\u17b6\u1794\u17cb (\u1782\u17d2\u179a\u17bc\u179f\u17b6\u179f\u17d2\u179a\u17cd)',
            'attendance.students': '\u179f\u17b7\u179f\u17d2\u179f\u17b8',
            'grades.title': '\u1793\u17b7\u1791\u17d2\u1791\u17c1\u179f',
            'grades.subtitle': 'GPA \u1793\u17b7\u1784\u1798\u17b7\u1787\u17d2\u1787\u1798\u17b6\u1793\u1798\u17bb\u1781\u179c\u17b7\u1787\u17b6',
            'scores.title': '\u1796\u17b7\u1793\u17d2\u1791\u17bb',
            'scores.subtitle': '\u1796\u17b7\u1793\u17d2\u1791\u17bb\u1790\u17d2\u1793\u17b6\u1780\u17cb \u1793\u17b7\u1784\u1793\u17b6\u17c6\u1785\u17bc\u179b/\u1793\u17b6\u1793\u17c5\u1785\u17bc\u179b',
            'users.salary': 'ប្រាក់ខែ',
            'requests.inbox': 'ប្រអប់សំណើ',
            'requests.mine': 'សំណើរបស់ខ្មែរ',
            'requests.subtitle': 'បញ្ជាក់និងសំណើសម្គេត',
            'requests.allStatuses': 'សថាភាពទាំងអស់',
            'requests.new': 'សំណើថ្មី',
            'requests.subject': 'បទាន',
            'requests.from': 'ពី',
            'requests.category': 'ប្រភេទ',
            'requests.created': 'បង្កើត',
            'requests.open': 'បាន',
            'requests.empty': 'មិនទាន់មានសំណើ។',
            'grades.gpaLocked': 'ត្រូវការអនុញ្ញាតមើល GPA',
            'grades.requestGpa': 'ស្នើសុំមើល GPA',
            'requests.approveGpa': 'អនុម័ត GPA',
            'requests.rejectGpa': 'បដិសេធ',
            'finance.title': 'ហិរញ្ញវត្ថុ',
            'finance.subtitleStaff': 'ខេតប្រាក់និងបំណិល',
            'finance.subtitleMine': 'បំណិលគណាងខ្មែរ',
            'finance.addPayroll': 'បន្ថៃមខេតប្រាក់',
            'finance.addPayment': 'បន្ថៃមបំណិល',
            'finance.payroll': 'ខេតប្រាក់',
            'finance.payments': 'បំណិល',
            'finance.user': 'អ្នកប្រើ',
            'finance.period': 'រយៈពេល',
            'finance.amount': 'ចំនួនទឹងទាន',
            'finance.note': 'កំណត់ថ្មាស',
            'finance.type': 'ប្រភេទ',
            'finance.due': 'កាលកំណត់',
            'finance.emptyPayroll': 'មិនមានខេតប្រាក់។',
            'finance.emptyPayments': 'មិនមានបំណិល។',
            'scores.search': 'ស្វែងរកសិស្សី អូ មុខវិជា...',
            'scores.allSessions': 'ពាក់សិស្សីទាំងអស់',
            'scores.selectClass': 'ជាស់រស់ថ្នាក់',
            'scores.export': 'ទាញយក Excel',
            'scores.import': 'នាំចូល Excel',
            'scores.add': 'បន្ថែមពិន្ទុមួយ',
            'scores.sessionBtn': 'បញ្ចូលពិន្ទុ',
            'scores.inputTitle': 'បញ្ចូលពិន្ទុ',
            'scores.inputSubtitle': 'បញ្ចូលពិន្ទុសម្រាប់សិស្សម្នាក់ៗ រួចរក្សាទុក។ រក្សាទុកតាមមុខវិជ្ជា និងពាក់។',
            'scores.sessionOpen': 'បើកតារាងពិន្ទុថ្នាក់',
            'scores.sessionTitle': 'បញ្ចូលពិន្ទុថ្នាក់',
            'scores.sessionSubtitle': 'បង្ហាញសិស្សទាំងអស់ក្នុងថ្នាក់ បញ្ចូលពិន្ទុ រួចរក្សាទុកម្តង',
            'scores.sessionHint': 'បំពេញក្រឡាដែលត្រូវការ រួចចុច រក្សាទុកពិន្ទុទាំងអស់ ម្តង។',
            'scores.pickHint': 'ជ្រើសរើសវគ្គសិក្សា និងថ្នាក់ ដើម្បីមើលពិន្ទុ។',
            'scores.session': 'ពាក់សិស្ស',
            'scores.subject': 'មុខវិជ្ជាសិក្សា',
            'scores.allSubjects': 'មុខវិជ្ជាទាំងអស់',
            'scores.subjectPlaceholder': 'ឧ. គណិតវិទ្យា',
            'scores.term': 'ពាក់',
            'scores.score': 'ពិន្ទុ',
            'scores.maxScore': 'ពិន្ទុអតិបរមា',
            'scores.remark': 'កំណត់ចំណាំ',
            'scores.remarkOptional': 'ស្រេចចិត្ត',
            'scores.loadSheet': 'ផ្ទុកតារាង',
            'scores.saveSession': 'រក្សាទុកពិន្ទុទាំងអស់',
            'scores.noStudents': 'មិនមានសិស្សក្នុងថ្នាក់នេះ។',
            'scores.sessionPickClass': 'ជ្រើសរើសថ្នាក់ដើម្បីផ្ទុកសិស្សទាំងអស់ក្នុងថ្នាក់។',
            'scores.sessionPickSubject': 'ជ្រើសរើសមុខវិជ្ជា (ឬទាំងអស់) ដើម្បីបើកតារាងពិន្ទុ។',
            'scores.noClassSubjects': 'ថ្នាក់នេះមិនទាន់មានមុខវិជ្ជា។',
            'scores.editClassSubjects': 'បន្ថែមមុខវិជ្ជាថ្នាក់',
            'scores.teacher': 'គ្រូសាស្រ៍',
            'scores.empty': 'មិនមានពិន្ទុសម្រាប់ថ្នាក់/ពាក់។',
        }
    };

    function readTheme() {
        try { return localStorage.getItem(THEME_KEY) === 'dark' ? 'dark' : 'light'; }
        catch (_) { return 'light'; }
    }

    function readLang() {
        try { return localStorage.getItem(LANG_KEY) === 'km' ? 'km' : 'en'; }
        catch (_) { return 'en'; }
    }

    function langLabel(lang) {
        return lang === 'km' ? '\u1781\u17d2\u1798\u17c2\u179a' : 'EN';
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        try { localStorage.setItem(THEME_KEY, theme); } catch (_) {}
        document.querySelectorAll('[data-theme-toggle]').forEach((btn) => {
            const next = theme === 'dark' ? 'light' : 'dark';
            btn.setAttribute('aria-label', next === 'dark' ? 'Switch to dark theme' : 'Switch to light theme');
            btn.setAttribute('title', next === 'dark' ? 'Dark' : 'Light');
        });
    }

    function t(lang, key) {
        return (dict[lang] && dict[lang][key]) || (dict.en && dict.en[key]) || key;
    }

    function translate(lang) {
        document.querySelectorAll('[data-i18n]').forEach((el) => {
            const key = el.getAttribute('data-i18n');
            if (!key) return;
            el.textContent = t(lang, key);
        });
        document.querySelectorAll('[data-i18n-placeholder]').forEach((el) => {
            const key = el.getAttribute('data-i18n-placeholder');
            if (key) el.setAttribute('placeholder', t(lang, key));
        });
        document.querySelectorAll('[data-i18n-title]').forEach((el) => {
            const key = el.getAttribute('data-i18n-title');
            if (key) el.setAttribute('title', t(lang, key));
        });
        document.querySelectorAll('[data-i18n-aria]').forEach((el) => {
            const key = el.getAttribute('data-i18n-aria');
            if (key) el.setAttribute('aria-label', t(lang, key));
        });
        document.dispatchEvent(new CustomEvent('sm:langchange', { detail: { lang: lang } }));
    }

    function applyLang(lang) {
        document.documentElement.setAttribute('data-lang', lang);
        document.documentElement.lang = lang === 'km' ? 'km' : 'en';
        try { localStorage.setItem(LANG_KEY, lang); } catch (_) {}
        document.querySelectorAll('[data-lang-label]').forEach((el) => {
            el.textContent = langLabel(lang);
        });
        document.querySelectorAll('[data-lang-option]').forEach((btn) => {
            const active = btn.getAttribute('data-lang-option') === lang;
            btn.classList.toggle('is-active', active);
            btn.setAttribute('aria-selected', active ? 'true' : 'false');
        });
        closeLangMenus();
        translate(lang);
    }

    function closeLangMenus() {
        document.querySelectorAll('[data-lang-menu]').forEach((menu) => {
            const dropdown = menu.querySelector('[data-lang-dropdown]');
            const trigger = menu.querySelector('[data-lang-toggle]');
            if (dropdown) dropdown.hidden = true;
            if (trigger) trigger.setAttribute('aria-expanded', 'false');
        });
    }

    function bind() {
        document.querySelectorAll('[data-theme-toggle]').forEach((btn) => {
            btn.addEventListener('click', () => {
                applyTheme(readTheme() === 'dark' ? 'light' : 'dark');
            });
        });

        document.querySelectorAll('[data-lang-menu]').forEach((menu) => {
            const trigger = menu.querySelector('[data-lang-toggle]');
            const dropdown = menu.querySelector('[data-lang-dropdown]');
            if (!trigger || !dropdown) return;

            trigger.addEventListener('click', (e) => {
                e.stopPropagation();
                const open = dropdown.hidden;
                closeLangMenus();
                dropdown.hidden = !open;
                trigger.setAttribute('aria-expanded', open ? 'true' : 'false');
            });

            menu.querySelectorAll('[data-lang-option]').forEach((opt) => {
                opt.addEventListener('click', (e) => {
                    e.stopPropagation();
                    applyLang(opt.getAttribute('data-lang-option'));
                });
            });
        });

        document.addEventListener('click', () => closeLangMenus());
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') closeLangMenus();
        });
    }

    function boot() {
        bind();
        applyTheme(readTheme());
        applyLang(readLang());
    }

    window.SmI18n = {
        t: function (key) { return t(readLang(), key); },
        apply: function () { translate(readLang()); },
        readLang: readLang
    };

    applyTheme(readTheme());
    document.documentElement.setAttribute('data-lang', readLang());
    document.documentElement.lang = readLang() === 'km' ? 'km' : 'en';

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }
})();
