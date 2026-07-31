
let sidebarOpen = false;
const sidebar = document.getElementById('sidebar');

function openSidebar() {
  if (!sidebarOpen) {
    sidebar.classList.add('sidebar-responsive');
    sidebarOpen = true;
  }
}

function closeSidebar() {
  if (sidebarOpen) {
    sidebar.classList.remove('sidebar-responsive');
    sidebarOpen = false;
  }
}

// ---------- CHARTS ----------

// BAR CHART
const barChartOptions = {
  series: [
    {
      data: [8, 7, 7, 5, 14, 8, 13, 10, 7, 5, 6, 5, 9, 9, 4, 4, 4, 10, 7, 11, 12, 8, 6, 5, 6, 3, 3, 7, 5, 8],
      name: 'Products',
    },
  ],
  chart: {
    type: 'bar',
    background: 'transparent',
    height: 350,
    width: 700,
    toolbar: {
      show: false,
    },
  },
  colors: ['#2962ff', '#583cb3', '#2962ff', '#583cb3', '#2962ff', '#583cb3', '#2962ff', '#583cb3', '#2962ff', '#583cb3', '#2962ff'
    , '#583cb3', '#2962ff', '#583cb3', '#2962ff', '#583cb3', '#2962ff', '#583cb3', '#2962ff', '#583cb3', '#2962ff', '#583cb3', '#2962ff'
    , '#583cb3', '#2962ff', '#583cb3', '#2962ff', '#583cb3', '#2962ff', '#583cb3'],

  plotOptions: {
    bar: {
      distributed: true,
      borderRadius: 4,
      horizontal: false,
      columnWidth: '40%',
    },
  },
  dataLabels: {
    enabled: false,
  },
  fill: {
    opacity: 1,
  },
  grid: {
    borderColor: '#f5f7ff',
    yaxis: {
      lines: {
        show: true,
      },
    },
    xaxis: {
      lines: {
        show: true,
      },
    },
  },
  legend: {
    labels: {
      colors: '#55596e',
    },
    show: false,
    position: 'top',
  },
  stroke: {
    colors: ['transparent'],
    show: true,
    width: 2,
  },
  tooltip: {
    shared: true,
    intersect: false,
    theme: 'dark',
  },
  xaxis: {
    categories: ['Zara', 'Bersheka', 'Guess', 'Channel', 'Kiko', 'Lacoste', 'Burberry', 'Adidas', 'Nike'
      , 'Gap', 'Miniso', 'Okaidi', 'R&B', 'Aldo', 'Almajed', 'Boss', 'Hub', 'NewYorker', 'Puma'
      , 'Virgin', 'Dior', 'ELC', 'Mango', 'Reebok', 'Faces', 'H&M', 'Skechers', 'ToysRus', 'BodyShop'
      , 'Nayomi'],
    title: {
      style: {
        color: '#f5f7ff',
      },
    },
    axisBorder: {
      show: true,
      color: '#f5f7ff',
    },
    axisTicks: {
      show: true,
      color: '#f5f7ff',
    },
    labels: {
      style: {
        colors: '#55596e',
      },
    },
  },
  yaxis: {
    title: {
      text: 'No.items',
      style: {
        color: '#55596e',
      },
    },
    axisBorder: {
      color: '#f5f7ff',
      show: true,
    },
    axisTicks: {
      color: '#f5f7ff',
      show: true,
    },
    labels: {
      style: {
        colors: '#55596e',
      },
    },
  },
};

const barChart = new ApexCharts(
  document.querySelector('#bar-chart'),
  barChartOptions
);
barChart.render();


var ctx2 = document.getElementById('doughnut').getContext('2d');
var myChart2 = new Chart(ctx2, {
  type: 'doughnut',
  data: {
    labels: ['Ahmed Omar', 'Kareem Salem', 'Mazen Ali', 'Khalid Saleh'],

    datasets: [{
      label: 'Collectors',
      data: [10, 3, 7, 5],
      backgroundColor: [
        'rgba(54, 162, 235, 1)',
        '#89aede',
        '#e85092',
        'rgba(120, 46, 139,1)'

      ],
      borderColor: [
        '#fefeff',
        '#fefeff',
        '#fefeff',
        '#fefeff'

      ],
      borderWidth: 1
    }]

  },
  options: {
    responsive: true
  }
});